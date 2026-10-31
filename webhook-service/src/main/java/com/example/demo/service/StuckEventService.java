package com.example.demo.service;

import com.example.demo.entity.WebhookEvent;
import com.example.demo.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class StuckEventService {

    private final WebhookEventRepository events;

    public StuckEventService(WebhookEventRepository events) {
        this.events = events;
    }

    @Transactional
    public List<String> claimStuck(Duration minAge) {
        Instant now = Instant.now();
        List<WebhookEvent> stuck =
                events.findStuck(WebhookEvent.Status.RECEIVED, now.minus(minAge));
        stuck.forEach(e -> e.setLastSweptAt(now));      // đánh dấu đã nhặt
        return stuck.stream().map(WebhookEvent::getEventId).toList();
    }
}
