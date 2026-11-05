package com.settledesk.event;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TradeCreatedEvent(

        @NotNull(message = "Trade ID is required")
        Long tradeId,

        @NotBlank(message = "Instrument is required")
        String instrument,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal quantity,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal price,

        @NotBlank(message = "Buyer is required")
        String buyer,

        @NotBlank(message = "Seller is required")
        String seller,

        @NotBlank(message = "Status is required")
        String status

) {
}