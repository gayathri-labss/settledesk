package com.settledesk.event;

import com.settledesk.model.SettlementStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementFailedEvent(
        @NotNull Long settlementId,
        @NotNull Long tradeId,
        @NotNull String settlementReference,
        @NotNull LocalDate settlementDate,
        @NotNull String failureReason,
        @NotNull SettlementStatus status,
        @NotNull LocalDateTime failedAt
) {}