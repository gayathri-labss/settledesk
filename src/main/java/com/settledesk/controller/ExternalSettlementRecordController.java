package com.settledesk.controller;

import com.settledesk.dto.CreateExternalSettlementRequest;
import com.settledesk.model.ExternalSettlementRecord;
import com.settledesk.service.ExternalSettlementRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ExternalSettlementRecordController {

    private final ExternalSettlementRecordService service;

    @PostMapping("/external-records")
    public ResponseEntity<ExternalSettlementRecord> createExternalRecord(
            @Valid @RequestBody CreateExternalSettlementRequest request) {

        ExternalSettlementRecord savedRecord =
                service.createExternalRecord(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedRecord);
    }

    @GetMapping("/external-records")
    public ResponseEntity<List<ExternalSettlementRecord>> getAllExternalRecords() {
        return ResponseEntity.ok(service.getAllExternalRecords());
    }

    @GetMapping("/external-records/{id}")
    public ResponseEntity<ExternalSettlementRecord> getExternalRecordById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getExternalRecordById(id));
    }
}