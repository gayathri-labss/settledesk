package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_break")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationBreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long breakId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false, unique = true)
    private ReconciliationResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BreakCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BreakStatus status = BreakStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BreakPriority priority = BreakPriority.MEDIUM;

    @Column(length = 2000)
    private String description;

    private String assignedTo;

    @Column(length = 2000)
    private String resolutionNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}