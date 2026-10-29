package com.example.demo.service;

import com.example.demo.dto.WebhookRequest;
import com.example.demo.producer.PaymentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    public enum Result { ACCEPTED, DUPLICATE }

    private final WebhookReceiver receiver;
    private final PaymentEventPublisher publisher;

    public WebhookService(WebhookReceiver receiver, PaymentEventPublisher publisher) {
        this.receiver = receiver;
        this.publisher = publisher;
    }

    public Result handle(WebhookRequest req) {
        try {
            receiver.store(req);
        } catch (DataIntegrityViolationException e) {
            return Result.DUPLICATE;
        }

        try {
            publisher.publish(req.eventId());
        } catch (AmqpException e) {
            log.warn("Could not publish event {}; it stays RECEIVED", req.eventId(), e);
        }
        return Result.ACCEPTED;
    }
}
