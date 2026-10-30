package com.example.demo.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitConfig {

    public static final String PAYMENT_QUEUE = "payment.events";
    public static final String DLQ = "payment.events.dlq";
    public static final int MAX_RETRIES = 3;
    private static final int[] RETRY_DELAYS_MS = {2000, 4000, 8000};

    public static String retryQueue(int attempt) {
        return PAYMENT_QUEUE + ".retry." + attempt;
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Declarables retryQueues() {
        List<Queue> queues = new ArrayList<>();
        for (int i = 1; i <= MAX_RETRIES; i++) {
            queues.add(QueueBuilder.durable(retryQueue(i))
                    .ttl(RETRY_DELAYS_MS[i - 1])        // chờ bao lâu
                    .deadLetterExchange("")             // hết giờ thì đẩy qua exchange mặc định...
                    .deadLetterRoutingKey(PAYMENT_QUEUE) // ...với routing key = hàng chính
                    .build());
        }
        return new Declarables(queues);
    }
}