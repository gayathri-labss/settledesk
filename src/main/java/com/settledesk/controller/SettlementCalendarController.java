package com.settledesk.controller;

import com.settledesk.dto.SettlementCalendarResponse;
import com.settledesk.service.SettlementCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settlement-calendars")
@RequiredArgsConstructor
public class SettlementCalendarController {

    private final SettlementCalendarService settlementCalendarService;

    @GetMapping
    public ResponseEntity<List<SettlementCalendarResponse>> getAllSettlementCalendars() {

        return ResponseEntity.ok(
                settlementCalendarService.getAllSettlementCalendars()
        );
    }
}
