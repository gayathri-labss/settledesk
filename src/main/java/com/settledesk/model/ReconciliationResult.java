package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_result")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ReconciliationRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    private SettlementInstruction internalSettlement;

    private String externalReference;
    private String externalSettlementReference;
    private String externalInstrument;
    private BigDecimal externalQuantity;
    private BigDecimal externalPrice;
    private LocalDate externalSettlementDate;
    private String externalStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationResultStatus status;

    @Column(length = 2000)
    private String mismatchDetails;

    private LocalDateTime reconciledAt;
}
