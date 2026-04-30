package com.smartsupport.dto;

import com.smartsupport.entity.Ticket;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class TicketDTO {

    // ─── Request DTO (what client sends) ────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "Title cannot be blank")
        private String title;

        @NotBlank(message = "Description cannot be blank")
        private String description;

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid email format")
        private String customerEmail;

        private String customerName;

        // Optional; AI classifier will override if not provided
        private Ticket.TicketCategory category;
    }

    // ─── Update DTO ───────────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private Ticket.TicketStatus status;
        private Ticket.TicketPriority priority;
        private Ticket.TicketCategory category;
        private String assignedAgent;
    }

    // ─── Response DTO (what API returns) ─────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private String customerEmail;
        private String customerName;
        private Ticket.TicketStatus status;
        private Ticket.TicketPriority priority;
        private Ticket.TicketCategory category;
        private Double urgencyScore;
        private String aiSuggestion;
        private String assignedAgent;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime resolvedAt;

        // Converts entity → DTO
        public static Response from(Ticket ticket) {
            return Response.builder()
                    .id(ticket.getId())
                    .title(ticket.getTitle())
                    .description(ticket.getDescription())
                    .customerEmail(ticket.getCustomerEmail())
                    .customerName(ticket.getCustomerName())
                    .status(ticket.getStatus())
                    .priority(ticket.getPriority())
                    .category(ticket.getCategory())
                    .urgencyScore(ticket.getUrgencyScore())
                    .aiSuggestion(ticket.getAiSuggestion())
                    .assignedAgent(ticket.getAssignedAgent())
                    .createdAt(ticket.getCreatedAt())
                    .updatedAt(ticket.getUpdatedAt())
                    .resolvedAt(ticket.getResolvedAt())
                    .build();
        }
    }

    // ─── Analytics DTO ────────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalyticsResponse {
        private long totalTickets;
        private long openTickets;
        private long resolvedTickets;
        private long criticalTickets;
        private double avgResolutionTimeHours;
        private java.util.Map<String, Long> ticketsByCategory;
        private java.util.Map<String, Long> ticketsByPriority;
        private java.util.Map<String, Long> ticketsByStatus;
    }

    // ─── AI Classifier Response ───────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIClassificationResult {
        private String category;
        private String priority;
        private Double urgencyScore;
        private String suggestion;
    }
}
