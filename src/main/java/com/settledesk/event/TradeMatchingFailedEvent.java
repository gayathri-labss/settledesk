package com.settledesk.event;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record TradeMatchingFailedEvent(
        @NotNull Long tradeId,
        @NotNull Long matchingRunId,
        String failureReason,
        @NotNull LocalDateTime failedAt
) {}