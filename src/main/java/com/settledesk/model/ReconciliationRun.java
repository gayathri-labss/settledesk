package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long runId;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationRunStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Integer totalInternalRecords;
    private Integer totalExternalRecords;
    private Integer matchedCount;
    private Integer mismatchedCount;
    private Integer missingExternalCount;
    private Integer missingInternalCount;
}