package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.entity.WebhookEvent;
import com.example.demo.exception.NonRetryableException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    WebhookEventRepository events;
    @Mock
    OrderRepository orders;
    @Mock
    PaymentRepository payments;
    @InjectMocks
    PaymentProcessor processor;

    @Test
    void marksOrderPaidAndEventProcessed() {
        WebhookEvent event = new WebhookEvent("evt_1", "payment.succeeded", 1L, 150000L);
        Order order = new Order(150000L);
        when(events.findByEventId("evt_1")).thenReturn(Optional.of(event));
        when(orders.findById(1L)).thenReturn(Optional.of(order));

        processor.process("evt_1");

        assertThat(order.getStatus()).isEqualTo(Order.Status.PAID);
        assertThat(event.getStatus()).isEqualTo(WebhookEvent.Status.PROCESSED);
        verify(payments).save(any(Payment.class));
    }

    @Test
    void skipsEventThatIsAlreadyProcessed() {
        WebhookEvent event = new WebhookEvent("evt_1", "payment.succeeded", 1L, 150000L);
        event.setStatus(WebhookEvent.Status.PROCESSED);
        when(events.findByEventId("evt_1")).thenReturn(Optional.of(event));

        processor.process("evt_1");

        verifyNoInteractions(orders, payments);
    }

    @Test
    void failsWithRetryableErrorWhenOrderIsMissing() {
        WebhookEvent event = new WebhookEvent("evt_1", "payment.succeeded", 1L, 150000L);
        when(events.findByEventId("evt_1")).thenReturn(Optional.of(event));
        when(orders.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.process("evt_1"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(payments);
    }

    @Test
    void failsWithNonRetryableErrorOnAmountMismatch() {
        WebhookEvent event = new WebhookEvent("evt_1", "payment.succeeded", 1L, 999L);
        Order order = new Order(150000L);
        when(events.findByEventId("evt_1")).thenReturn(Optional.of(event));
        when(orders.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> processor.process("evt_1"))
                .isInstanceOf(NonRetryableException.class);
        verifyNoInteractions(payments);
    }
}
