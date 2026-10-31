package com.example.demo.repository;

import com.example.demo.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    Optional<WebhookEvent> findByEventId(String eventId);
    List<WebhookEvent> findByStatus(WebhookEvent.Status status);

    @Query("""
       select e from WebhookEvent e
       where e.status = :status
         and e.receivedAt < :before
         and (e.lastSweptAt is null or e.lastSweptAt < :before)
       """)
    List<WebhookEvent> findStuck(@Param("status") WebhookEvent.Status status,
                                 @Param("before") Instant before);
}