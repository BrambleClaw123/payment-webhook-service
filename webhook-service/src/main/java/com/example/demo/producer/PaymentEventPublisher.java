package com.example.demo.producer;

import com.example.demo.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbit;

    public PaymentEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publish(String eventId) {
        rabbit.convertAndSend(RabbitConfig.PAYMENT_QUEUE, eventId);
    }
}