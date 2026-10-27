package com.example.demo.controller;

import com.example.demo.dto.WebhookRequest;
import com.example.demo.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final WebhookService service;

    public WebhookController(WebhookService service) {
        this.service = service;
    }

    @PostMapping("/payment")
    public Map<String, String> receive(@Valid @RequestBody WebhookRequest req) {
        WebhookService.Result result = service.handle(req);
        return Map.of("eventId", req.eventId(), "status", result.name());
    }
}
