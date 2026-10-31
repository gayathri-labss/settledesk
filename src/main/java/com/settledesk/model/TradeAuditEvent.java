package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeAuditAction action;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime performedAt;
}