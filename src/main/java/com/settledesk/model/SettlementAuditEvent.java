package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private SettlementInstruction settlement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementAuditAction action;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime performedAt;
}