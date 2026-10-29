package com.example.demo.consumer;

import com.example.demo.config.RabbitConfig;
import com.example.demo.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
 public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentProcessor processor;

    public PaymentEventConsumer(PaymentProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    public void onMessage(String eventId) {
        log.info("Processing event {}", eventId);
        processor.process(eventId);
    }
}
