package com.settledesk.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade_lifecycle")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lifecycleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @Enumerated(EnumType.STRING)
    private TradeStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus newStatus;

    @Column(nullable = false)
    private LocalDateTime changedAt;
}