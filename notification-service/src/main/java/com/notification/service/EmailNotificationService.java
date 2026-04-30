package com.notification.service;

import com.notification.kafka.TicketEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    // ─── Ticket Created ───────────────────────────────────────────────────────

    public void sendTicketCreatedEmail(TicketEventMessage event) {
        String subject = "[SmartSupport] Ticket #" + event.getTicketId() + " Created Successfully";
        String body = """
                Dear %s,
                
                Your support ticket has been received and classified by our AI system.
                
                ──────────────────────────────────
                Ticket ID    : #%d
                Title        : %s
                Category     : %s
                Priority     : %s
                Urgency Score: %.0f%%
                Status       : %s
                ──────────────────────────────────
                
                💡 AI Suggested Resolution:
                %s
                
                Our team will reach out to you within the SLA timeframe.
                
                Best regards,
                SmartSupport Team
                """.formatted(
                        event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                        event.getTicketId(),
                        event.getTitle(),
                        event.getCategory(),
                        event.getPriority(),
                        event.getUrgencyScore() != null ? event.getUrgencyScore() * 100 : 0,
                        event.getStatus(),
                        event.getAiSuggestion()
                );

        sendEmail(event.getCustomerEmail(), subject, body);
    }

    // ─── Ticket Resolved ──────────────────────────────────────────────────────

    public void sendTicketResolvedEmail(TicketEventMessage event) {
        String subject = "[SmartSupport] Ticket #" + event.getTicketId() + " Has Been Resolved ✅";
        String body = """
                Dear %s,
                
                Great news! Your support ticket #%d has been resolved.
                
                Title: %s
                
                We hope this resolves your concern. Please reply to this email if you need further assistance.
                
                Thank you for your patience.
                
                Best regards,
                SmartSupport Team
                """.formatted(
                        event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                        event.getTicketId(),
                        event.getTitle()
                );

        sendEmail(event.getCustomerEmail(), subject, body);
    }

    // ─── Ticket Assigned ──────────────────────────────────────────────────────

    public void sendTicketAssignedEmail(TicketEventMessage event) {
        String subject = "[SmartSupport] Ticket #" + event.getTicketId() + " Assigned to an Agent";
        String body = """
                Dear %s,
                
                Your ticket #%d has been assigned to %s.
                They will contact you shortly.
                
                Best regards,
                SmartSupport Team
                """.formatted(
                        event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                        event.getTicketId(),
                        event.getAssignedAgent() != null ? event.getAssignedAgent() : "our support team"
                );

        sendEmail(event.getCustomerEmail(), subject, body);
    }

    // ─── Escalation ──────────────────────────────────────────────────────────

    public void sendEscalationEmail(TicketEventMessage event) {
        String subject = "[SmartSupport] ⚠ Ticket #" + event.getTicketId() + " Has Been Escalated";
        String body = """
                Dear %s,
                
                We sincerely apologize for the delay. Your ticket #%d has been escalated to our senior support team.
                
                Title   : %s
                Priority: %s
                
                You will receive a response within 2 hours.
                
                Best regards,
                SmartSupport Team
                """.formatted(
                        event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                        event.getTicketId(),
                        event.getTitle(),
                        event.getPriority()
                );

        sendEmail(event.getCustomerEmail(), subject, body);
    }

    // ─── Critical Alert (internal team email) ─────────────────────────────────

    public void sendCriticalAlertEmail(TicketEventMessage event) {
        String subject = "🚨 CRITICAL TICKET ALERT: #" + event.getTicketId();
        String body = """
                CRITICAL TICKET REQUIRES IMMEDIATE ATTENTION
                
                Ticket ID    : #%d
                Customer     : %s (%s)
                Title        : %s
                Urgency Score: %.0f%%
                Priority     : %s
                Category     : %s
                
                AI Suggestion: %s
                
                Please act immediately.
                """.formatted(
                        event.getTicketId(),
                        event.getCustomerName(),
                        event.getCustomerEmail(),
                        event.getTitle(),
                        event.getUrgencyScore() != null ? event.getUrgencyScore() * 100 : 0,
                        event.getPriority(),
                        event.getCategory(),
                        event.getAiSuggestion()
                );

        // In real prod, send to support team DL
        sendEmail("support-team@company.com", subject, body);
        log.error("Critical alert email sent for ticket #{}", event.getTicketId());
    }

    // ─── Generic Send ─────────────────────────────────────────────────────────

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@smartsupport.com");
            mailSender.send(message);
            log.info("Email sent to {} | Subject: {}", to, subject);
        } catch (Exception e) {
            // Don't crash the consumer if email fails; log and continue
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
