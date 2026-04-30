package com.smartsupport.controller;

import com.smartsupport.dto.TicketDTO;
import com.smartsupport.entity.Ticket;
import com.smartsupport.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
public class TicketController {

    private final TicketService ticketService;

    // ─── POST /api/v1/tickets ─────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<TicketDTO.Response> createTicket(
            @Valid @RequestBody TicketDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request));
    }

    // ─── GET /api/v1/tickets ──────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<TicketDTO.Response>> getAllTickets(
            @RequestParam(required = false) Ticket.TicketStatus status,
            @RequestParam(required = false) String customerEmail) {

        if (status != null) {
            return ResponseEntity.ok(ticketService.getTicketsByStatus(status));
        }
        if (customerEmail != null) {
            return ResponseEntity.ok(ticketService.getTicketsByCustomer(customerEmail));
        }
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    // ─── GET /api/v1/tickets/{id} ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO.Response> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    // ─── PUT /api/v1/tickets/{id} ─────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<TicketDTO.Response> updateTicket(
            @PathVariable Long id,
            @RequestBody TicketDTO.UpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }

    // ─── DELETE /api/v1/tickets/{id} ─────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    // ─── GET /api/v1/tickets/analytics ───────────────────────────────────────
    @GetMapping("/analytics")
    public ResponseEntity<TicketDTO.AnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(ticketService.getAnalytics());
    }
}
