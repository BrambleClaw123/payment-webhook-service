package com.example.demo.service;

import com.example.demo.config.RabbitConfig;
import com.example.demo.producer.PaymentEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReplayService {

    private final RabbitTemplate rabbit;
    private final WebhookEventAdmin admin;
    private final PaymentEventPublisher publisher;

    public ReplayService(RabbitTemplate rabbit, WebhookEventAdmin admin,
                         PaymentEventPublisher publisher) {
        this.rabbit = rabbit;
        this.admin = admin;
        this.publisher = publisher;
    }

    public int replay(int limit) {
        int count = 0;
        for (int i = 0; i < limit; i++) {
            Object body = rabbit.receiveAndConvert(RabbitConfig.DLQ, 200);
            if (body == null) break;                 // DLQ đã hết tin
            String eventId = body.toString();
            admin.resetForReplay(eventId);           // FAILED → RECEIVED (đã commit)
            publisher.publish(eventId);              // đưa lại vào hàng chính
            count++;
        }
        return count;
    }
}