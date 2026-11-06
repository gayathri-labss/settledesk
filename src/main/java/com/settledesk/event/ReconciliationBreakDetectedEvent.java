package com.settledesk.event;

import com.settledesk.model.BreakCategory;
import com.settledesk.model.BreakPriority;
import com.settledesk.model.BreakStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReconciliationBreakDetectedEvent(
        @NotNull Long breakId,
        @NotNull Long runId,
        @NotNull BreakCategory category,
        @NotNull BreakStatus status,
        @NotNull BreakPriority priority,
        String description,
        String internalSettlementReference,
        String externalReference,
        String externalSettlementReference,
        @NotNull LocalDateTime detectedAt
) {}