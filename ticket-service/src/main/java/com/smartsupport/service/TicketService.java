package com.smartsupport.service;

import com.smartsupport.dto.TicketDTO;
import com.smartsupport.entity.Ticket;
import com.smartsupport.exception.TicketNotFoundException;
import com.smartsupport.kafka.TicketEvent;
import com.smartsupport.kafka.TicketEventProducer;
import com.smartsupport.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AIClassifierService aiClassifierService;
    private final TicketEventProducer eventProducer;

    // ─── Create ──────────────────────────────────────────────────────────────

    @Transactional
    public TicketDTO.Response createTicket(TicketDTO.CreateRequest request) {
        // 1. Build ticket entity
        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .status(Ticket.TicketStatus.OPEN)
                .priority(Ticket.TicketPriority.MEDIUM) // default; AI will override
                .category(request.getCategory())
                .build();

        // 2. Call AI classifier
        TicketDTO.AIClassificationResult ai =
                aiClassifierService.classify(request.getTitle(), request.getDescription());

        ticket.setUrgencyScore(ai.getUrgencyScore());
        ticket.setAiSuggestion(ai.getSuggestion());
        ticket.setPriority(Ticket.TicketPriority.valueOf(ai.getPriority()));
        if (request.getCategory() == null) {
            ticket.setCategory(Ticket.TicketCategory.valueOf(ai.getCategory()));
        }

        // 3. Auto-escalate critical tickets
        if (ticket.getPriority() == Ticket.TicketPriority.CRITICAL) {
            ticket.setStatus(Ticket.TicketStatus.ESCALATED);
        }

        // 4. Save to DB
        Ticket saved = ticketRepository.save(ticket);

        // 5. Publish Kafka event
        TicketEvent event = buildEvent(saved, TicketEvent.EventType.TICKET_CREATED);
        eventProducer.publishTicketEvent(event);

        if (saved.getPriority() == Ticket.TicketPriority.CRITICAL) {
            eventProducer.publishCriticalAlert(event);
        }

        log.info("Ticket created: id={} priority={} urgency={}",
                saved.getId(), saved.getPriority(), saved.getUrgencyScore());

        return TicketDTO.Response.from(saved);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public TicketDTO.Response getTicketById(Long id) {
        return TicketDTO.Response.from(findOrThrow(id));
    }

    public List<TicketDTO.Response> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(TicketDTO.Response::from)
                .collect(Collectors.toList());
    }

    public List<TicketDTO.Response> getTicketsByStatus(Ticket.TicketStatus status) {
        return ticketRepository.findByStatus(status).stream()
                .map(TicketDTO.Response::from)
                .collect(Collectors.toList());
    }

    public List<TicketDTO.Response> getTicketsByCustomer(String email) {
        return ticketRepository.findByCustomerEmail(email).stream()
                .map(TicketDTO.Response::from)
                .collect(Collectors.toList());
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Transactional
    public TicketDTO.Response updateTicket(Long id, TicketDTO.UpdateRequest request) {
        Ticket ticket = findOrThrow(id);

        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
            if (request.getStatus() == Ticket.TicketStatus.RESOLVED) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }
        if (request.getPriority() != null)      ticket.setPriority(request.getPriority());
        if (request.getCategory() != null)      ticket.setCategory(request.getCategory());
        if (request.getAssignedAgent() != null) ticket.setAssignedAgent(request.getAssignedAgent());

        Ticket updated = ticketRepository.save(ticket);

        // Determine event type
        TicketEvent.EventType eventType = request.getStatus() == Ticket.TicketStatus.RESOLVED
                ? TicketEvent.EventType.TICKET_RESOLVED
                : request.getAssignedAgent() != null
                ? TicketEvent.EventType.TICKET_ASSIGNED
                : TicketEvent.EventType.TICKET_UPDATED;

        eventProducer.publishTicketEvent(buildEvent(updated, eventType));

        return TicketDTO.Response.from(updated);
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = findOrThrow(id);
        ticketRepository.delete(ticket);
        log.info("Ticket deleted: id={}", id);
    }

    // ─── Analytics ───────────────────────────────────────────────────────────

    public TicketDTO.AnalyticsResponse getAnalytics() {
        long total    = ticketRepository.count();
        long open     = ticketRepository.countByStatus(Ticket.TicketStatus.OPEN);
        long resolved = ticketRepository.countByStatus(Ticket.TicketStatus.RESOLVED);
        long critical = ticketRepository.countByPriority(Ticket.TicketPriority.CRITICAL);
        Double avgResolution = ticketRepository.findAverageResolutionTimeHours();

        Map<String, Long> byCategory = toStringMap(ticketRepository.countByCategory());
        Map<String, Long> byPriority = toStringMap(ticketRepository.countByPriority());
        Map<String, Long> byStatus   = toStringMap(ticketRepository.countByStatus());

        return TicketDTO.AnalyticsResponse.builder()
                .totalTickets(total)
                .openTickets(open)
                .resolvedTickets(resolved)
                .criticalTickets(critical)
                .avgResolutionTimeHours(avgResolution != null ? avgResolution : 0)
                .ticketsByCategory(byCategory)
                .ticketsByPriority(byPriority)
                .ticketsByStatus(byStatus)
                .build();
    }

    // ─── Scheduled: auto-escalate stale tickets every hour ──────────────────

    @Scheduled(fixedRate = 3_600_000) // every 1 hour
    @Transactional
    public void autoEscalateStaleTickets() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Ticket> stale = ticketRepository.findByCreatedAtBefore(threshold).stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.OPEN
                          && t.getPriority() == Ticket.TicketPriority.HIGH)
                .toList();

        stale.forEach(ticket -> {
            ticket.setStatus(Ticket.TicketStatus.ESCALATED);
            ticketRepository.save(ticket);
            eventProducer.publishTicketEvent(buildEvent(ticket, TicketEvent.EventType.TICKET_ESCALATED));
            log.warn("Auto-escalated stale ticket id={}", ticket.getId());
        });
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private Ticket findOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
    }

    private TicketEvent buildEvent(Ticket ticket, TicketEvent.EventType type) {
        return TicketEvent.builder()
                .eventType(type)
                .ticketId(ticket.getId())
                .title(ticket.getTitle())
                .customerEmail(ticket.getCustomerEmail())
                .customerName(ticket.getCustomerName())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .urgencyScore(ticket.getUrgencyScore())
                .assignedAgent(ticket.getAssignedAgent())
                .aiSuggestion(ticket.getAiSuggestion())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private Map<String, Long> toStringMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return result;
    }
}
