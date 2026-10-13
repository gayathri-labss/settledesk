package com.settledesk.controller;

import com.settledesk.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.settledesk.dto.CreateTradeRequest;
import com.settledesk.model.Trade;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final TradeService tradeService;

    @Autowired
    public TradeController (TradeService tradeService){
        this.tradeService = tradeService;
    }

    @PostMapping
    public ResponseEntity<Trade> createTrade( @Valid @RequestBody CreateTradeRequest request){
        Trade trade = tradeService.createTrade(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(trade);
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<Trade> getTradeById(@PathVariable Long tradeId){
        Trade trade = tradeService.getTradeByTradeId(tradeId);
        return ResponseEntity.ok(trade);
    }
}
