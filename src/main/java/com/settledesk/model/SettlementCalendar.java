package com.settledesk.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "settlement_calendar",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_settlement_calendar_code",
                columnNames = "calendar_code"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class SettlementCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long calendarId;

    @Column(name = "calendar_code", nullable = false, unique = true, length = 30)
    private String calendarCode;

    @Column(name = "calendar_name", nullable = false, length = 150)
    private String calendarName;

    @Column(nullable = false, length = 50)
    private String market;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "time_zone", nullable = false, length = 60)
    private String timeZone;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
