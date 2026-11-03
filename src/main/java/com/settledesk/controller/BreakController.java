package com.settledesk.controller;

import com.settledesk.dto.AssignBreakRequest;
import com.settledesk.dto.BreakResponse;
import com.settledesk.dto.ResolveBreakRequest;
import com.settledesk.dto.UpdateBreakRequest;
import com.settledesk.model.*;
import com.settledesk.service.BreakService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/breaks")
@RequiredArgsConstructor
@Validated
public class BreakController {

    private final BreakService breakService;

    @GetMapping
    public ResponseEntity<Page<BreakResponse>> searchBreaks(
            @RequestParam(required = false) BreakStatus status,
            @RequestParam(required = false) BreakCategory category,
            @RequestParam(required = false) Long runId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok(
                breakService.searchBreaks(
                        status, category, runId, page, size)
        );
    }

    @GetMapping("/{breakId}")
    public ResponseEntity<BreakResponse> getBreakById(
            @PathVariable Long breakId) {

        return ResponseEntity.ok(
                breakService.getBreakById(breakId)
        );
    }

    @PatchMapping("/{breakId}")
    public ResponseEntity<BreakResponse> updateBreak(
            @PathVariable Long breakId,
            @Valid @RequestBody UpdateBreakRequest request) {

        BreakResponse updatedBreak =
                breakService.updateBreak(breakId, request);

        return ResponseEntity.ok(updatedBreak);
    }

    @PostMapping("/{breakId}/resolve")
    public ResponseEntity<BreakResponse> resolveBreak(
            @PathVariable Long breakId,
            @Valid @RequestBody ResolveBreakRequest request) {

        BreakResponse resolvedBreak =
                breakService.resolveBreak(breakId, request);

        return ResponseEntity.ok(resolvedBreak);
    }

    @PostMapping("/{breakId}/assign")
    public ResponseEntity<BreakResponse> assignBreak(
            @PathVariable Long breakId,
            @Valid @RequestBody AssignBreakRequest request) {

        BreakResponse assignedBreak =
                breakService.assignBreak(breakId, request);

        return ResponseEntity.ok(assignedBreak);
    }
}