package com.settledesk.controller;

import com.settledesk.dto.AuditEventResponse;
import com.settledesk.service.AuditEventService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventService auditEventService;

    @GetMapping
    public ResponseEntity<List<AuditEventResponse>> getAllAuditEvents() {

        return ResponseEntity.ok(
                auditEventService.getAllAuditEvents()
        );
    }
}