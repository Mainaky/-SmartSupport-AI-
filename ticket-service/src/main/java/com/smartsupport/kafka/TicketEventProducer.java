package com.smartsupport.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEventProducer {

    private static final String TOPIC_TICKET_EVENTS = "ticket-events";
    private static final String TOPIC_CRITICAL_ALERTS = "critical-ticket-alerts";

    private final KafkaTemplate<String, TicketEvent> kafkaTemplate;

    /**
     * Publishes a ticket event to the main ticket-events topic.
     * Key = ticketId ensures all events for a ticket go to the same partition (ordering).
     */
    public void publishTicketEvent(TicketEvent event) {
        String key = String.valueOf(event.getTicketId());
        CompletableFuture<SendResult<String, TicketEvent>> future =
                kafkaTemplate.send(TOPIC_TICKET_EVENTS, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: type={} ticketId={} partition={} offset={}",
                        event.getEventType(),
                        event.getTicketId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: type={} ticketId={} error={}",
                        event.getEventType(), event.getTicketId(), ex.getMessage());
            }
        });
    }

    /**
     * Publishes to a high-priority critical alerts topic for CRITICAL tickets.
     * Notification service can have a dedicated consumer for this.
     */
    public void publishCriticalAlert(TicketEvent event) {
        kafkaTemplate.send(TOPIC_CRITICAL_ALERTS, String.valueOf(event.getTicketId()), event);
        log.warn("CRITICAL ALERT published for ticketId={} urgencyScore={}",
                event.getTicketId(), event.getUrgencyScore());
    }
}
