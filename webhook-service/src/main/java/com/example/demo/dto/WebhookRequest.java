package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WebhookRequest(
        @NotBlank String eventId,
        @NotBlank String type,
        @NotNull Long orderId,
        @NotNull @Positive Long amount
) {}
