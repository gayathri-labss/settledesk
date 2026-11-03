package com.settledesk.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "external_settlement_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_source_reference",
                        columnNames = {"source", "external_reference"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long externalRecordId;

    @Column(nullable = false)
    private String externalReference;

    @Column(nullable = false)
    private String settlementReference;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String instrument;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExternalSettlementStatus status;

    @Column(nullable = false)
    private LocalDateTime receivedAt;
}
