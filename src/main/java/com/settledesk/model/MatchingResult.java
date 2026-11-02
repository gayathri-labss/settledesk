package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "matching_result",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"run_id", "trade_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private MatchingRun matchingRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingOutcome outcome;

    @Column(length = 2000)
    private String mismatchDetails;

    @Column(nullable = false)
    private LocalDateTime evaluatedAt;
}