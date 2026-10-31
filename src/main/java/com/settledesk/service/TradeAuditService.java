package com.settledesk.service;

import com.settledesk.model.Trade;
import com.settledesk.model.TradeAuditAction;
import com.settledesk.model.TradeAuditEvent;
import com.settledesk.repository.TradeAuditRepository;
import com.settledesk.repository.TradeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradeAuditService {

    private final TradeAuditRepository tradeAuditRepository;
    private final TradeRepository tradeRepository;

    public TradeAuditService(
            TradeAuditRepository tradeAuditRepository,
            TradeRepository tradeRepository) {

        this.tradeAuditRepository = tradeAuditRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional
    public void recordAudit(
            Trade trade,
            TradeAuditAction action,
            String details) {

        TradeAuditEvent auditEvent = TradeAuditEvent.builder()
                .trade(trade)
                .action(action)
                .details(details)
                .performedAt(LocalDateTime.now())
                .build();

        tradeAuditRepository.save(auditEvent);
    }

    @Transactional(readOnly = true)
    public List<TradeAuditEvent> getTradeAuditHistory(Long tradeId) {

        if (!tradeRepository.existsById(tradeId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Trade not found with id: " + tradeId
            );
        }

        return tradeAuditRepository
                .findByTrade_TradeIdOrderByPerformedAtAsc(tradeId);
    }
}