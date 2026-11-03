package com.settledesk.controller;

import com.settledesk.dto.StartReconciliationRequest;
import com.settledesk.model.ReconciliationRun;
import com.settledesk.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/runs")
    public ResponseEntity<ReconciliationRun> startReconciliation(
            @Valid @RequestBody StartReconciliationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reconciliationService.startReconciliation(request));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<ReconciliationRun>> getAllReconciliationRuns() {
        return ResponseEntity.ok(
                reconciliationService.getAllReconciliationRuns()
        );
    }
}
