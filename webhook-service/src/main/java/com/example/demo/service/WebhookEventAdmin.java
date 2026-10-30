package com.example.demo.service;

import com.example.demo.entity.WebhookEvent;
import com.example.demo.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WebhookEventAdmin {

    private final WebhookEventRepository events;

    public WebhookEventAdmin(WebhookEventRepository events) {
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<WebhookEvent> findFailed() {
        return events.findByStatus(WebhookEvent.Status.FAILED);
    }

    @Transactional
    public void resetForReplay(String eventId) {
        events.findByEventId(eventId).ifPresent(e -> {
            if (e.getStatus() == WebhookEvent.Status.FAILED) {
                e.setStatus(WebhookEvent.Status.RECEIVED);
            }
        });
    }
}
