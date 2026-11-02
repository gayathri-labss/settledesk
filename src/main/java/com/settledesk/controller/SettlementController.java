package com.settledesk.controller;

import com.settledesk.dto.ConfirmSettlementRequest;
import com.settledesk.dto.CreateSettlementRequest;
import com.settledesk.model.SettlementAuditEvent;
import com.settledesk.model.SettlementInstruction;
import com.settledesk.model.SettlementLifecycleEvent;
import com.settledesk.service.SettlementService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<SettlementInstruction> createSettlement(
            @Valid @RequestBody CreateSettlementRequest request) {

        SettlementInstruction instruction =
                settlementService.createSettlementInstruction(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(instruction);
    }

    @GetMapping
    public ResponseEntity<List<SettlementInstruction>> getAllSettlements() {

        List<SettlementInstruction> settlements =
                settlementService.getAllSettlements();

        return ResponseEntity.ok(settlements);
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementInstruction> getSettlementById(
            @PathVariable Long settlementId) {

        SettlementInstruction settlement =
                settlementService.getSettlementById(settlementId);

        return ResponseEntity.ok(settlement);
    }

    @PostMapping("/{settlementId}/submit")
    public ResponseEntity<SettlementInstruction> submitSettlement(
            @PathVariable Long settlementId) {

        SettlementInstruction instruction =
                settlementService.submitSettlement(settlementId);

        return ResponseEntity.ok(instruction);
    }

    @PostMapping("/{settlementId}/confirm")
    public ResponseEntity<SettlementInstruction> confirmSettlement(
            @PathVariable Long settlementId,
            @Valid @RequestBody ConfirmSettlementRequest request) {

        SettlementInstruction instruction =
                settlementService.confirmSettlement(settlementId, request);

        return ResponseEntity.ok(instruction);
    }

    @PostMapping("/{settlementId}/retry")
    public ResponseEntity<SettlementInstruction> retrySettlement(
            @PathVariable Long settlementId) {

        SettlementInstruction instruction =
                settlementService.retrySettlement(settlementId);

        return ResponseEntity.ok(instruction);
    }

    @GetMapping("/{settlementId}/status-history")
    public ResponseEntity<List<SettlementLifecycleEvent>> getSettlementStatusHistory(
            @PathVariable Long settlementId) {

        List<SettlementLifecycleEvent> history =
                settlementService.getSettlementStatusHistory(settlementId);

        return ResponseEntity.ok(history);
    }

    @GetMapping("/{settlementId}/audit")
    public ResponseEntity<List<SettlementAuditEvent>> getSettlementAuditHistory(
            @PathVariable Long settlementId) {

        return ResponseEntity.ok(
                settlementService.getSettlementAuditHistory(settlementId)
        );
    }
}