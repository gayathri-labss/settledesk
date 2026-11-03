package com.settledesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SettlementCalendarResponse {

    private Long calendarId;
    private String calendarCode;
    private String calendarName;
    private String market;
    private String country;
    private String currency;
    private String timeZone;
    private boolean active;
}
