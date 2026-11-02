package com.settledesk.controller;

import com.settledesk.dto.CreateSettlementRequest;
import com.settledesk.model.SettlementInstruction;
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
}