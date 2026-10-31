package com.settledesk.service;

import com.settledesk.model.Trade;
import com.settledesk.model.TradeStatus;
import com.settledesk.model.TradeLifecycleEvent;
import com.settledesk.repository.TradeLifecycleRepository;
import com.settledesk.repository.TradeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeLifecycleService {

    private final TradeLifecycleRepository lifecycleRepository;
    private final TradeRepository tradeRepository;

    public TradeLifecycleService(
            TradeLifecycleRepository lifecycleRepository,
            TradeRepository tradeRepository) {

        this.lifecycleRepository = lifecycleRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional
    public void recordStatusChange(
            Trade trade,
            TradeStatus previousStatus,
            TradeStatus newStatus) {

        TradeLifecycleEvent event = TradeLifecycleEvent.builder()
                .trade(trade)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .build();

        lifecycleRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<TradeLifecycleEvent> getTradeLifecycle(Long tradeId) {

        if (!tradeRepository.existsById(tradeId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Trade not found with id: " + tradeId
            );
        }

        return lifecycleRepository
                .findByTrade_TradeIdOrderByChangedAtAsc(tradeId);
    }
}