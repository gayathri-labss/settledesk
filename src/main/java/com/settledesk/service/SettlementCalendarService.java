package com.settledesk.service;

import com.settledesk.dto.SettlementCalendarResponse;
import com.settledesk.model.SettlementCalendar;
import com.settledesk.repository.SettlementCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementCalendarService {

    private final SettlementCalendarRepository settlementCalendarRepository;

    public List<SettlementCalendarResponse> getAllSettlementCalendars() {

        return settlementCalendarRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SettlementCalendarResponse toResponse(SettlementCalendar calendar) {

        return SettlementCalendarResponse.builder()
                .calendarId(calendar.getCalendarId())
                .calendarCode(calendar.getCalendarCode())
                .calendarName(calendar.getCalendarName())
                .market(calendar.getMarket())
                .country(calendar.getCountry())
                .currency(calendar.getCurrency())
                .timeZone(calendar.getTimeZone())
                .active(calendar.isActive())
                .build();
    }
}
