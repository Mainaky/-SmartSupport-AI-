package com.smartsupport.service;

import com.smartsupport.dto.TicketDTO;
import com.smartsupport.entity.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Calls the Python AI microservice (port 5000) to classify
 * tickets by category, priority, and urgency score.
 *
 * Falls back to a rule-based heuristic if the AI service is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIClassifierService {

    @Value("${ai.classifier.url:http://localhost:5000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;

    public TicketDTO.AIClassificationResult classify(String title, String description) {
        try {
            return callAIService(title, description);
        } catch (Exception e) {
            log.warn("AI service unavailable ({}), using rule-based fallback.", e.getMessage());
            return ruleBasedFallback(title, description);
        }
    }

    private TicketDTO.AIClassificationResult callAIService(String title, String description) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("description", description);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<TicketDTO.AIClassificationResult> response = restTemplate.exchange(
                aiServiceUrl + "/classify",
                HttpMethod.POST,
                request,
                TicketDTO.AIClassificationResult.class
        );

        log.info("AI classification result: {}", response.getBody());
        return response.getBody();
    }

    /**
     * Simple keyword-based fallback when Python service is down.
     * Good enough for demo; real prod would use a cached model.
     */
    private TicketDTO.AIClassificationResult ruleBasedFallback(String title, String description) {
        String text = (title + " " + description).toLowerCase();

        // Determine category
        String category;
        if (text.matches(".*\\b(bill|invoice|payment|charge|refund|money)\\b.*")) {
            category = Ticket.TicketCategory.BILLING.name();
        } else if (text.matches(".*\\b(error|bug|crash|not working|broken|fail|issue)\\b.*")) {
            category = Ticket.TicketCategory.TECHNICAL.name();
        } else if (text.matches(".*\\b(account|login|password|access|locked)\\b.*")) {
            category = Ticket.TicketCategory.ACCOUNT.name();
        } else if (text.matches(".*\\b(complain|terrible|worst|awful|unacceptable)\\b.*")) {
            category = Ticket.TicketCategory.COMPLAINT.name();
        } else if (text.matches(".*\\b(feature|suggest|enhancement|would like|improve)\\b.*")) {
            category = Ticket.TicketCategory.FEATURE_REQUEST.name();
        } else {
            category = Ticket.TicketCategory.GENERAL.name();
        }

        // Determine urgency score
        double urgencyScore = 0.3; // default low
        if (text.contains("urgent") || text.contains("asap") || text.contains("critical")) {
            urgencyScore = 0.95;
        } else if (text.contains("important") || text.contains("immediately")) {
            urgencyScore = 0.75;
        } else if (text.contains("soon") || text.contains("quickly")) {
            urgencyScore = 0.55;
        }

        // Determine priority based on urgency score
        String priority;
        if (urgencyScore >= 0.85) {
            priority = Ticket.TicketPriority.CRITICAL.name();
        } else if (urgencyScore >= 0.65) {
            priority = Ticket.TicketPriority.HIGH.name();
        } else if (urgencyScore >= 0.45) {
            priority = Ticket.TicketPriority.MEDIUM.name();
        } else {
            priority = Ticket.TicketPriority.LOW.name();
        }

        // Generic suggestion
        String suggestion = getSuggestionForCategory(category);

        return new TicketDTO.AIClassificationResult(category, priority, urgencyScore, suggestion);
    }

    private String getSuggestionForCategory(String category) {
        return switch (category) {
            case "BILLING" -> "Verify the payment transaction and check if refund policy applies. Escalate to billing team if unresolved within 24h.";
            case "TECHNICAL" -> "Collect error logs from the customer. Check the known-issues board. Attempt cache clear and re-login steps first.";
            case "ACCOUNT" -> "Verify customer identity, trigger password reset flow. If locked, manually unlock from admin panel.";
            case "COMPLAINT" -> "Acknowledge with empathy. Escalate to senior support. Offer a service credit if appropriate.";
            case "FEATURE_REQUEST" -> "Log to product backlog. Acknowledge receipt to customer and set expectation on timeline.";
            default -> "Route to general support queue. Respond within SLA.";
        };
    }
}
