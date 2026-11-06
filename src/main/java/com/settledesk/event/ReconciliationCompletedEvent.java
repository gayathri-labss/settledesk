package com.settledesk.event;

import com.settledesk.model.ReconciliationRunStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReconciliationCompletedEvent(
        @NotNull Long runId,
        @NotNull String source,
        @NotNull LocalDate settlementDate,
        @NotNull ReconciliationRunStatus status,
        @NotNull Integer totalInternalRecords,
        @NotNull Integer totalExternalRecords,
        @NotNull Integer matchedCount,
        @NotNull Integer mismatchedCount,
        @NotNull Integer missingExternalCount,
        @NotNull Integer missingInternalCount,
        @NotNull LocalDateTime startedAt,
        @NotNull LocalDateTime completedAt
) {}