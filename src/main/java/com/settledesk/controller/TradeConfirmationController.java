package com.settledesk.controller;

import com.settledesk.dto.CreateTradeConfirmationRequest;
import com.settledesk.model.TradeConfirmation;
import com.settledesk.service.TradeConfirmationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trades/{tradeId}/confirmations")
public class TradeConfirmationController {

    private final TradeConfirmationService tradeConfirmationService;

    public TradeConfirmationController(
            TradeConfirmationService tradeConfirmationService) {

        this.tradeConfirmationService = tradeConfirmationService;
    }

    @PostMapping
    public ResponseEntity<TradeConfirmation> createConfirmation(
            @PathVariable Long tradeId,
            @Valid @RequestBody CreateTradeConfirmationRequest request) {

        TradeConfirmation confirmation =
                tradeConfirmationService.createConfirmation(tradeId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(confirmation);
    }

    @GetMapping
    public ResponseEntity<TradeConfirmation> getConfirmationByTradeId(
            @PathVariable Long tradeId) {

        TradeConfirmation confirmation =
                tradeConfirmationService.getConfirmationByTradeId(tradeId);

        return ResponseEntity.ok(confirmation);
    }
}