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
}
