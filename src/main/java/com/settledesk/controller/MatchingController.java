package com.settledesk.controller;

import com.settledesk.dto.MatchingRunRequest;
import com.settledesk.model.MatchingResult;
import com.settledesk.model.MatchingRun;
import com.settledesk.service.MatchingService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/runs")
    public ResponseEntity<List<MatchingRun>> getAllMatchingRuns() {
        List<MatchingRun> matchingRuns = matchingService.getAllMatchingRuns();
        return ResponseEntity.ok(matchingRuns);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<MatchingRun> getMatchingRunById(
            @PathVariable Long runId) {

        MatchingRun matchingRun = matchingService.getMatchingRunById(runId);

        return ResponseEntity.ok(matchingRun);
    }

    @GetMapping("/runs/{runId}/results")
    public ResponseEntity<List<MatchingResult>> getMatchingResultsByRunId(
            @PathVariable Long runId) {

        List<MatchingResult> results =
                matchingService.getMatchingResultsByRunId(runId);

        return ResponseEntity.ok(results);
    }
}