package com.example.demo.service;

import com.example.demo.dto.WebhookRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {

    public enum Result { PROCESSED, DUPLICATE }

    private final PaymentProcessor processor;

    public WebhookService(PaymentProcessor processor) {
        this.processor = processor;
    }

    public Result handle(WebhookRequest req) {
        try {
            processor.process(req);
            return Result.PROCESSED;
        } catch (DataIntegrityViolationException e) {
            return Result.DUPLICATE;     // vi phạm UNIQUE → đây là bản trùng
        }
    }
}
