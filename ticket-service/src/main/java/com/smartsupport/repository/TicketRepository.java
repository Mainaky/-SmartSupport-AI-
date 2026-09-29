package com.smartsupport.repository;

import com.smartsupport.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(Ticket.TicketStatus status);

    List<Ticket> findByCustomerEmail(String customerEmail);

    List<Ticket> findByPriority(Ticket.TicketPriority priority);

    List<Ticket> findByCategory(Ticket.TicketCategory category);

    List<Ticket> findByAssignedAgent(String agentName);

    List<Ticket> findByStatusAndPriority(Ticket.TicketStatus status, Ticket.TicketPriority priority);

    long countByStatus(Ticket.TicketStatus status);

    long countByPriority(Ticket.TicketPriority priority);

    long countByCategory(Ticket.TicketCategory category);

    // Tickets created in last N hours
    List<Ticket> findByCreatedAtBefore(LocalDateTime dateTime);

    // Tickets not yet assigned (unassigned open tickets)
    List<Ticket> findByAssignedAgentIsNullAndStatus(Ticket.TicketStatus status);

    // Average resolution time for resolved tickets (in hours)
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.resolvedAt)) FROM Ticket t WHERE t.resolvedAt IS NOT NULL")
    Double findAverageResolutionTimeHours();

    // Group count by category
    @Query("SELECT t.category, COUNT(t) FROM Ticket t GROUP BY t.category")
    List<Object[]> countByCategory();

    // Group count by priority
    @Query("SELECT t.priority, COUNT(t) FROM Ticket t GROUP BY t.priority")
    List<Object[]> countByPriority();

    // Group count by status
    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countByStatus();

    // High urgency unresolved tickets
    @Query("SELECT t FROM Ticket t WHERE t.urgencyScore >= :threshold AND t.status NOT IN ('RESOLVED','CLOSED')")
    List<Ticket> findHighUrgencyOpen(double threshold);
}
