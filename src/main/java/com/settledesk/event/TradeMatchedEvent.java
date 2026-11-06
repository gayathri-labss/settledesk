package com.settledesk.event;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TradeMatchedEvent(
        @NotNull Long tradeId,
        @NotNull Long matchingRunId,
        @NotNull LocalDateTime matchedAt
) {}