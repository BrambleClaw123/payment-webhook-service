package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
public class WebhookEvent {
    public enum Status { RECEIVED, PROCESSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    private String type;
    private Long orderId;
    private Long amount;

    @Enumerated(EnumType.STRING)
    private Status status = Status.RECEIVED;

    private Instant receivedAt = Instant.now();

    protected WebhookEvent() {}
    public WebhookEvent(String eventId, String type, Long orderId, Long amount) {
        this.eventId = eventId;
        this.type = type;
        this.orderId = orderId;
        this.amount = amount;
    }

    public void setStatus(Status status) { this.status = status; }
}