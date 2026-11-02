package com.settledesk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateSettlementRequest {

    @NotNull(message = "Trade ID is required")
    @Positive(message = "Trade ID must be positive")
    private Long tradeId;

    @NotNull(message = "Settlement date is required")
    private LocalDate settlementDate;
}