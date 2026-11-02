package com.settledesk.controller;

import com.settledesk.dto.MatchingRunRequest;
import com.settledesk.model.MatchingRun;
import com.settledesk.service.MatchingService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/runs")
    public ResponseEntity<MatchingRun> startMatchingRun(
            @Valid @RequestBody MatchingRunRequest request) {

        MatchingRun matchingRun =
                matchingService.startMatchingRun(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(matchingRun);
    }
}