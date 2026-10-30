package com.example.demo.consumer;

import com.example.demo.config.RabbitConfig;
import com.example.demo.exception.NonRetryableException;
import com.example.demo.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private static final String RETRY_HEADER = "retry-count";

    private final PaymentProcessor processor;
    private final RabbitTemplate rabbit;

    public PaymentEventConsumer(PaymentProcessor processor, RabbitTemplate rabbit) {
        this.processor = processor;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    public void onMessage(Message message) {
        String eventId = new String(message.getBody(), StandardCharsets.UTF_8);
        int retries = readRetries(message);
        log.info("Processing event {} (retries so far: {})", eventId, retries);

        try {
            processor.process(eventId);
        } catch (NonRetryableException e) {
            log.error("Event {} failed permanently: {}", eventId, e.getMessage());
            sendToDlq(eventId, retries, e);
        } catch (Exception e) {
            if (retries >= RabbitConfig.MAX_RETRIES) {
                log.error("Event {} gave up after {} retries: {}", eventId, retries, e.getMessage());
                sendToDlq(eventId, retries, e);
            } else {
                int next = retries + 1;
                log.warn("Event {} failed ({}); scheduling retry #{}", eventId, e.getMessage(), next);
                rabbit.convertAndSend(RabbitConfig.retryQueue(next), eventId, m -> {
                    m.getMessageProperties().setHeader(RETRY_HEADER, next);
                    return m;
                });
            }
        }
    }

    private void sendToDlq(String eventId, int retries, Exception e) {
        processor.markFailed(eventId);
        rabbit.convertAndSend(RabbitConfig.DLQ, eventId, m -> {
            m.getMessageProperties().setHeader(RETRY_HEADER, retries);
            m.getMessageProperties().setHeader("error", String.valueOf(e.getMessage()));
            return m;
        });
    }

    private int readRetries(Message message) {
        List<Map<String, ?>> deaths = message.getMessageProperties().getXDeathHeader();
        if (deaths == null) {
            return 0;
        }
        String prefix = RabbitConfig.PAYMENT_QUEUE + ".retry.";
        int max = 0;
        for (Map<String, ?> d : deaths) {
            Object queue = d.get("queue");
            if (queue != null && queue.toString().startsWith(prefix)) {
                try {
                    int n = Integer.parseInt(queue.toString().substring(prefix.length()));
                    max = Math.max(max, n);
                } catch (NumberFormatException ignored) {
                    // bỏ qua queue có tên lạ
                }
            }
        }
        return max;
    }
}
