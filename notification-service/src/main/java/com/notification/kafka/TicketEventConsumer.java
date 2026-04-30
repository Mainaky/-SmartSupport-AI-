package com.notification.kafka;

import com.notification.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventConsumer {

    private final EmailNotificationService emailService;

    /**
     * Main consumer: listens to all ticket events.
     * Group ID = notification-service (independent from other consumers).
     */
    @KafkaListener(
        topics = "ticket-events",
        groupId = "notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTicketEvent(
            @Payload TicketEventMessage event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed event: type={} ticketId={} partition={} offset={}",
                event.getEventType(), event.getTicketId(), partition, offset);

        switch (event.getEventType()) {
            case "TICKET_CREATED"   -> emailService.sendTicketCreatedEmail(event);
            case "TICKET_RESOLVED"  -> emailService.sendTicketResolvedEmail(event);
            case "TICKET_ASSIGNED"  -> emailService.sendTicketAssignedEmail(event);
            case "TICKET_ESCALATED" -> emailService.sendEscalationEmail(event);
            default -> log.debug("No email action for event type: {}", event.getEventType());
        }
    }

    /**
     * Critical alert consumer — dedicated consumer for high-priority tickets.
     * Could trigger SMS or PagerDuty in real prod.
     */
    @KafkaListener(
        topics = "critical-ticket-alerts",
        groupId = "critical-alert-handler"
    )
    public void consumeCriticalAlert(@Payload TicketEventMessage event) {
        log.error("🚨 CRITICAL TICKET ALERT: id={} customer={} urgency={}",
                event.getTicketId(), event.getCustomerEmail(), event.getUrgencyScore());
        emailService.sendCriticalAlertEmail(event);
        // In real prod: integrate with PagerDuty, SMS, Slack webhook, etc.
    }
}
