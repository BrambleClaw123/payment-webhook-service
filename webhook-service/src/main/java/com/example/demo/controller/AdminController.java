package com.example.demo.controller;

import com.example.demo.service.ReplayService;
import com.example.demo.service.WebhookEventAdmin;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dlq")
public class AdminController {

    public record FailedEventView(String eventId, Long orderId, Long amount, String status) {}

    private final WebhookEventAdmin admin;
    private final ReplayService replayService;

    public AdminController(WebhookEventAdmin admin, ReplayService replayService) {
        this.admin = admin;
        this.replayService = replayService;
    }

    @GetMapping
    public List<FailedEventView> list() {
        return admin.findFailed().stream()
                .map(e -> new FailedEventView(e.getEventId(), e.getOrderId(),
                        e.getAmount(), e.getStatus().name()))
                .toList();
    }

    @PostMapping("/replay")
    public Map<String, Integer> replay(@RequestParam(defaultValue = "100") int limit) {
        return Map.of("replayed", replayService.replay(limit));
    }
}
