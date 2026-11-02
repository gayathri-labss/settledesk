package com.settledesk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trade_confirmation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long confirmationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false, unique = true)
    private Trade trade;

    @Column(nullable = false)
    private String counterpartyReference;

    @Column(nullable = false)
    private String instrument;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String buyer;

    @Column(nullable = false)
    private String seller;

    @Column(nullable = false)
    private LocalDateTime receivedAt;
}
