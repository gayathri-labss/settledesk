package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "instrument",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_instrument_code",
                        columnNames = "instrument_code"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long instrumentId;

    @Column(name = "instrument_code", nullable = false, length = 30)
    private String instrumentCode;

    @Column(name = "instrument_name", nullable = false, length = 150)
    private String instrumentName;

    @Column(name = "instrument_type", nullable = false, length = 50)
    private String instrumentType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 12)
    private String isin;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}