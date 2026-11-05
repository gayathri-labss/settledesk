package com.settledesk.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TradeConfirmedEvent(

        @NotNull(message = "Trade ID is required")
        Long tradeId,

        @NotBlank(message = "Confirmation reference is required")
        String confirmationReference,

        @NotBlank(message = "Confirmed by is required")
        String confirmedBy

) {
}