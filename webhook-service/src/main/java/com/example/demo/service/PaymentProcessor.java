package com.example.demo.service;

import com.example.demo.dto.WebhookRequest;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.entity.WebhookEvent;
import com.example.demo.exception.NonRetryableException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.WebhookEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentProcessor {

    private final WebhookEventRepository events;
    private final OrderRepository orders;
    private final PaymentRepository payments;

    public PaymentProcessor(WebhookEventRepository events,
                            OrderRepository orders,
                            PaymentRepository payments) {
        this.events = events;
        this.orders = orders;
        this.payments = payments;
    }

    @Transactional
    public void process(String eventId) {
        WebhookEvent event = events.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException("Event not found: " + eventId));

        if (event.getStatus() == WebhookEvent.Status.PROCESSED) {
            return; // tin giao lại lần hai: bỏ qua (consumer idempotent)
        }

        Order order = orders.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));

        if (!order.getAmount().equals(event.getAmount())) {
            throw new NonRetryableException("Amount mismatch for event " + eventId);
        }

        order.setStatus(Order.Status.PAID);
        payments.save(new Payment(order.getId(), event.getAmount(), eventId));
        event.setStatus(WebhookEvent.Status.PROCESSED);
    }

    @Transactional
    public void markFailed(String eventId) {
        events.findByEventId(eventId)
                .ifPresent(e -> e.setStatus(WebhookEvent.Status.FAILED));
    }
}
