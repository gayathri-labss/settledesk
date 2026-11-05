package com.settledesk.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeConfirmedEvent(

        @NotNull
        Long confirmationId,

        @NotNull
        Long tradeId,

        @NotBlank
        String counterpartyReference,

        @NotBlank
        String instrument,

        @NotNull
        @Positive
        BigDecimal quantity,

        @NotNull
        @Positive
        BigDecimal price,

        @NotBlank
        String buyer,

        @NotBlank
        String seller,

        @NotNull
        LocalDateTime receivedAt

) {
}