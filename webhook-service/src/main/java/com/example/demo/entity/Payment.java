package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long amount;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    private Instant createdAt = Instant.now();

    protected Payment() {}
    public Payment(Long orderId, Long amount, String eventId) {
        this.orderId = orderId;
        this.amount = amount;
        this.eventId = eventId;
    }
}