package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {
    public enum Status {PENDING, PAID}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    protected Order() {
    }

    public Order(Long amount) {
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public Long getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}