package com.example.demo.service;

import com.example.demo.dto.WebhookRequest;
import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.entity.WebhookEvent;
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
    public void process(WebhookRequest req) {
        // 1. Ghi event TRƯỚC. Nếu eventId trùng, DB ném lỗi ngay tại đây.
        WebhookEvent event = new WebhookEvent(
                req.eventId(), req.type(), req.orderId(), req.amount());
        events.saveAndFlush(event);

        // 2. Nghiệp vụ
        Order order = orders.findById(req.orderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getAmount().equals(req.amount())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Amount mismatch");
        }

        order.setStatus(Order.Status.PAID);
        payments.save(new Payment(order.getId(), req.amount(), req.eventId()));
        event.setStatus(WebhookEvent.Status.PROCESSED);
    }
}
