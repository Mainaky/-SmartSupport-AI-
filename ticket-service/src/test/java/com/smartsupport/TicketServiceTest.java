package com.smartsupport;

import com.smartsupport.dto.TicketDTO;
import com.smartsupport.entity.Ticket;
import com.smartsupport.exception.TicketNotFoundException;
import com.smartsupport.kafka.TicketEventProducer;
import com.smartsupport.repository.TicketRepository;
import com.smartsupport.service.AIClassifierService;
import com.smartsupport.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private AIClassifierService aiClassifierService;
    @Mock private TicketEventProducer eventProducer;

    @InjectMocks private TicketService ticketService;

    private TicketDTO.CreateRequest createRequest;
    private TicketDTO.AIClassificationResult aiResult;
    private Ticket savedTicket;

    @BeforeEach
    void setUp() {
        createRequest = new TicketDTO.CreateRequest(
                "Payment failed",
                "My payment was charged twice. This is urgent!",
                "customer@example.com",
                "John Doe",
                null
        );

        aiResult = new TicketDTO.AIClassificationResult(
                "BILLING", "HIGH", 0.75, "Check billing system for duplicate charges."
        );

        savedTicket = Ticket.builder()
                .id(1L)
                .title("Payment failed")
                .description("My payment was charged twice. This is urgent!")
                .customerEmail("customer@example.com")
                .customerName("John Doe")
                .status(Ticket.TicketStatus.OPEN)
                .priority(Ticket.TicketPriority.HIGH)
                .category(Ticket.TicketCategory.BILLING)
                .urgencyScore(0.75)
                .aiSuggestion("Check billing system for duplicate charges.")
                .build();
    }

    @Test
    void createTicket_shouldSaveAndPublishEvent() {
        when(aiClassifierService.classify(any(), any())).thenReturn(aiResult);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
        doNothing().when(eventProducer).publishTicketEvent(any());

        TicketDTO.Response response = ticketService.createTicket(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPriority()).isEqualTo(Ticket.TicketPriority.HIGH);
        assertThat(response.getCategory()).isEqualTo(Ticket.TicketCategory.BILLING);
        verify(ticketRepository, times(1)).save(any());
        verify(eventProducer, times(1)).publishTicketEvent(any());
    }

    @Test
    void getTicketById_shouldReturnTicket_whenExists() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(savedTicket));
        TicketDTO.Response response = ticketService.getTicketById(1L);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCustomerEmail()).isEqualTo("customer@example.com");
    }

    @Test
    void getTicketById_shouldThrow_whenNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ticketService.getTicketById(99L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllTickets_shouldReturnList() {
        when(ticketRepository.findAll()).thenReturn(List.of(savedTicket));
        List<TicketDTO.Response> result = ticketService.getAllTickets();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Payment failed");
    }

    @Test
    void updateTicket_shouldUpdateStatusAndPublishEvent() {
        TicketDTO.UpdateRequest updateReq = new TicketDTO.UpdateRequest(
                Ticket.TicketStatus.RESOLVED, null, null, "Alice"
        );
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(savedTicket));
        when(ticketRepository.save(any())).thenReturn(savedTicket);
        doNothing().when(eventProducer).publishTicketEvent(any());

        TicketDTO.Response response = ticketService.updateTicket(1L, updateReq);
        assertThat(response).isNotNull();
        verify(ticketRepository).save(any());
        verify(eventProducer).publishTicketEvent(any());
    }

    @Test
    void criticalTicket_shouldBeAutoEscalated() {
        TicketDTO.AIClassificationResult criticalAI = new TicketDTO.AIClassificationResult(
                "TECHNICAL", "CRITICAL", 0.95, "Immediate action required."
        );
        Ticket criticalTicket = Ticket.builder()
                .id(2L).title("System down").description("Everything broken")
                .customerEmail("dev@example.com").status(Ticket.TicketStatus.ESCALATED)
                .priority(Ticket.TicketPriority.CRITICAL).category(Ticket.TicketCategory.TECHNICAL)
                .urgencyScore(0.95).build();

        when(aiClassifierService.classify(any(), any())).thenReturn(criticalAI);
        when(ticketRepository.save(any())).thenReturn(criticalTicket);
        doNothing().when(eventProducer).publishTicketEvent(any());
        doNothing().when(eventProducer).publishCriticalAlert(any());

        TicketDTO.CreateRequest req = new TicketDTO.CreateRequest(
                "System down", "Everything broken", "dev@example.com", "Dev", null
        );
        TicketDTO.Response resp = ticketService.createTicket(req);

        assertThat(resp.getStatus()).isEqualTo(Ticket.TicketStatus.ESCALATED);
        verify(eventProducer).publishCriticalAlert(any());
    }
}
