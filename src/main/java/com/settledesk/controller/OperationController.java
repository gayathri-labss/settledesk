package com.settledesk.controller;

import com.settledesk.dto.OperationResponse;
import com.settledesk.service.OperationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @GetMapping("/{operationId}")
    public ResponseEntity<OperationResponse> getOperationById(
            @PathVariable Long operationId) {

        return ResponseEntity.ok(
                operationService.getOperationById(operationId)
        );
    }
}