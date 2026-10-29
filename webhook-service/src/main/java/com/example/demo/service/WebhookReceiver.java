package com.example.demo.service;

import com.example.demo.dto.WebhookRequest;
import com.example.demo.entity.WebhookEvent;
import com.example.demo.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookReceiver {

    private final WebhookEventRepository events;

    public WebhookReceiver(WebhookEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void store(WebhookRequest req) {
        events.saveAndFlush(new WebhookEvent(
                req.eventId(), req.type(), req.orderId(), req.amount()));
    }
}
