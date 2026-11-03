package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_lifecycle_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private SettlementInstruction settlement;

    @Enumerated(EnumType.STRING)
    private SettlementStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus newStatus;

    @Column(nullable = false)
    private LocalDateTime changedAt;
}