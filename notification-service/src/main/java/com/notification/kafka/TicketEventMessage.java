package com.notification.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mirror of TicketEvent from ticket-service.
 * Both services share the same JSON shape via Kafka.
 */
@Data
@NoArgsConstructor
public class TicketEventMessage {
    private String eventType;
    private Long ticketId;
    private String title;
    private String customerEmail;
    private String customerName;
    private String status;
    private String priority;
    private String category;
    private Double urgencyScore;
    private String assignedAgent;
    private String aiSuggestion;
    private LocalDateTime timestamp;
}
