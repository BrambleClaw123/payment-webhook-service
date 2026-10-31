package com.example.demo;

import com.example.demo.entity.Order;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.WebhookEventRepository;
import com.example.demo.entity.WebhookEvent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=false",
                "app.sweeper.enabled=false"
        })
@Testcontainers
class WebhookFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    @Value("${local.server.port}")
    int port;

    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired WebhookEventRepository events;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String webhookJson(String eventId, Long orderId, long amount) {
        return "{\"eventId\":\"" + eventId + "\",\"type\":\"payment.succeeded\","
                + "\"orderId\":" + orderId + ",\"amount\":" + amount + "}";
    }

    @Test
    void webhookMarksOrderAsPaid() throws Exception {
        Order order = orders.save(new Order(150000L));
        String eventId = "evt_" + UUID.randomUUID();

        HttpResponse<String> response =
                post("/webhooks/payment", webhookJson(eventId, order.getId(), 150000));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("ACCEPTED");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(orders.findById(order.getId()).orElseThrow().getStatus())
                        .isEqualTo(Order.Status.PAID));
        assertThat(payments.countByEventId(eventId)).isEqualTo(1);
    }

    @Test
    void fiveParallelDuplicatesCreateOnlyOnePayment() throws Exception {
        Order order = orders.save(new Order(150000L));
        String eventId = "evt_" + UUID.randomUUID();
        String json = webhookJson(eventId, order.getId(), 150000);

        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(pool.submit(() -> post("/webhooks/payment", json)));
        }
        int accepted = 0;
        int duplicate = 0;
        for (Future<HttpResponse<String>> future : futures) {
            String body = future.get().body();
            if (body.contains("ACCEPTED")) accepted++;
            if (body.contains("DUPLICATE")) duplicate++;
        }
        pool.shutdown();

        assertThat(accepted).isEqualTo(1);
        assertThat(duplicate).isEqualTo(4);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(orders.findById(order.getId()).orElseThrow().getStatus())
                        .isEqualTo(Order.Status.PAID));
        assertThat(payments.countByEventId(eventId)).isEqualTo(1);
    }

    @Test
    void amountMismatchEndsAsFailedWithoutPayment() throws Exception {
        Order order = orders.save(new Order(150000L));
        String eventId = "evt_" + UUID.randomUUID();

        post("/webhooks/payment", webhookJson(eventId, order.getId(), 999));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(events.findByEventId(eventId).orElseThrow().getStatus())
                        .isEqualTo(WebhookEvent.Status.FAILED));
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(Order.Status.PENDING);
        assertThat(payments.countByEventId(eventId)).isZero();
    }

    @Test
    void invalidPayloadReturns400() throws Exception {
        HttpResponse<String> response =
                post("/webhooks/payment", webhookJson("evt_bad", 1L, -5));

        assertThat(response.statusCode()).isEqualTo(400);
    }
}
