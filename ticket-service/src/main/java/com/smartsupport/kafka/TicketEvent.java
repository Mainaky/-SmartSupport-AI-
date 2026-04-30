package com.smartsupport.kafka;

import com.smartsupport.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka event payload published whenever a ticket changes state.
 * Consumed by notification-service and any other downstream services.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEvent {

    public enum EventType {
        TICKET_CREATED,
        TICKET_UPDATED,
        TICKET_ESCALATED,
        TICKET_RESOLVED,
        TICKET_ASSIGNED
    }

    private EventType eventType;
    private Long ticketId;
    private String title;
    private String customerEmail;
    private String customerName;
    private Ticket.TicketStatus status;
    private Ticket.TicketPriority priority;
    private Ticket.TicketCategory category;
    private Double urgencyScore;
    private String assignedAgent;
    private String aiSuggestion;
    private LocalDateTime timestamp;
}
