package com.settledesk.controller;

import com.settledesk.dto.BreakResponse;
import com.settledesk.model.*;
import com.settledesk.service.BreakService;
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
}