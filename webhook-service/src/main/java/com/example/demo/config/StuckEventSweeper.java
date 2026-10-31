package com.example.demo.config;

import com.example.demo.producer.PaymentEventPublisher;
import com.example.demo.service.StuckEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.sweeper.enabled", havingValue = "true", matchIfMissing = true)
public class StuckEventSweeper {

    private static final Logger log = LoggerFactory.getLogger(StuckEventSweeper.class);

    private final StuckEventService service;
    private final PaymentEventPublisher publisher;
    private final Duration minAge;

    public StuckEventSweeper(StuckEventService service,
                             PaymentEventPublisher publisher,
                             @Value("${app.sweeper.min-age-seconds:60}") long minAgeSeconds) {
        this.service = service;
        this.publisher = publisher;
        this.minAge = Duration.ofSeconds(minAgeSeconds);
    }

    @Scheduled(fixedDelayString = "${app.sweeper.interval-ms:30000}")
    public void sweep() {
        List<String> ids = service.claimStuck(minAge);
        if (ids.isEmpty()) {
            return;
        }
        log.info("Sweeper found {} stuck event(s), republishing", ids.size());
        for (String id : ids) {
            try {
                publisher.publish(id);
            } catch (AmqpException e) {
                log.warn("Sweeper could not republish {}", id, e);
            }
        }
    }
}
