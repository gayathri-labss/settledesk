package com.settledesk.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matching_run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingRunStatus status;
    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
    @Column(nullable = false)
    private Integer totalTrades;

    @Column(nullable = false)
    private Integer matchedCount;

    @Column(nullable = false)
    private Integer unmatchedCount;

    @Column(length = 1000)
    private String failureReason;
}
