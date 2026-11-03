
package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "counterparty",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_counterparty_code",
                        columnNames = "counterparty_code"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Counterparty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long counterpartyId;

    @Column(name = "counterparty_code", nullable = false, length = 50)
    private String counterpartyCode;

    @Column(name = "counterparty_name", nullable = false, length = 150)
    private String counterpartyName;

    @Column(name = "counterparty_type", nullable = false, length = 50)
    private String counterpartyType;

    @Column(nullable = false, length = 100)
    private String country;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
