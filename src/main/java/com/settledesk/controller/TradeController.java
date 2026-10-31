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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Set;
import com.settledesk.dto.UpdateTradeRequest;
import org.springframework.web.bind.annotation.PutMapping;
import com.settledesk.dto.UpdateTradeStatusRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import com.settledesk.service.TradeLifecycleService;
import com.settledesk.model.TradeLifecycleEvent;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final TradeService tradeService;
    private final TradeLifecycleService tradeLifecycleService;

    @Autowired
    public TradeController(
            TradeService tradeService,
            TradeLifecycleService tradeLifecycleService) {

        this.tradeService = tradeService;
        this.tradeLifecycleService = tradeLifecycleService;
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

    @PutMapping("/{tradeId}")
    public ResponseEntity<Trade> updateTrade(
            @PathVariable Long tradeId,
            @Valid @RequestBody UpdateTradeRequest request) {

        Trade updatedTrade = tradeService.updateTrade(tradeId, request);

        return ResponseEntity.ok(updatedTrade);
    }

    @PatchMapping("/{tradeId}/status")
    public ResponseEntity<Trade> updateTradeStatus(
            @PathVariable Long tradeId,
            @Valid @RequestBody UpdateTradeStatusRequest request) {

        Trade updatedTrade =
                tradeService.updateTradeStatus(tradeId, request);

        return ResponseEntity.ok(updatedTrade);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Trade>> createTrades(
            @RequestBody
            @NotEmpty(message = "At least one trade is required")
            List<@Valid CreateTradeRequest> requests) {

        List<Trade> trades = tradeService.createTrades(requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(trades);
    }

    @PostMapping("/{tradeId}/cancel")
    public ResponseEntity<Trade> cancelTrade(
            @PathVariable Long tradeId) {

        Trade cancelledTrade = tradeService.cancelTrade(tradeId);

        return ResponseEntity.ok(cancelledTrade);
    }

    @GetMapping("/{tradeId}/lifecycle")
    public ResponseEntity<List<TradeLifecycleEvent>> getTradeLifecycle(
            @PathVariable Long tradeId) {

        List<TradeLifecycleEvent> lifecycleEvents =
                tradeLifecycleService.getTradeLifecycle(tradeId);

        return ResponseEntity.ok(lifecycleEvents);
    }

    @GetMapping
    public ResponseEntity<Page<Trade>> searchTrades(
            @RequestParam(required = false) String instrument,
            @RequestParam(required = false) String buyer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "tradeId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Set<String> allowedSortFields = Set.of(
                "tradeId", "instrument", "buyer", "seller", "price", "quantity"
        );

        if (!allowedSortFields.contains(sortBy)) {
            return ResponseEntity.badRequest().build();
        }

        if (!direction.equalsIgnoreCase("asc")
                && !direction.equalsIgnoreCase("desc")) {
            return ResponseEntity.badRequest().build();
        }

        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Trade> trades =
                tradeService.searchTrades(instrument, buyer, pageable);

        return ResponseEntity.ok(trades);
    }
}
