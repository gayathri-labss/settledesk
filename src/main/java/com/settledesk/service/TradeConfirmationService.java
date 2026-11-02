package com.settledesk.service;

import com.settledesk.repository.TradeConfirmationRepository;
import com.settledesk.repository.TradeRepository;
import org.springframework.stereotype.Service;
import com.settledesk.dto.CreateTradeConfirmationRequest;
import com.settledesk.model.Trade;
import com.settledesk.model.TradeConfirmation;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TradeConfirmationService {

    private final TradeConfirmationRepository tradeConfirmationRepository;
    private final TradeRepository tradeRepository;

    public TradeConfirmationService(
            TradeConfirmationRepository tradeConfirmationRepository,
            TradeRepository tradeRepository) {

        this.tradeConfirmationRepository = tradeConfirmationRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional
    public TradeConfirmation createConfirmation(
            Long tradeId,
            CreateTradeConfirmationRequest request) {

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trade not found with id: " + tradeId
                ));

        if (tradeConfirmationRepository.existsByTrade_TradeId(tradeId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Confirmation already exists for trade: " + tradeId
            );
        }

        TradeConfirmation confirmation = TradeConfirmation.builder()
                .trade(trade)
                .counterpartyReference(request.getCounterpartyReference())
                .instrument(request.getInstrument())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .buyer(request.getBuyer())
                .seller(request.getSeller())
                .receivedAt(LocalDateTime.now())
                .build();

        return tradeConfirmationRepository.save(confirmation);
    }

    @Transactional(readOnly = true)
    public TradeConfirmation getConfirmationByTradeId(Long tradeId) {

        if (!tradeRepository.existsById(tradeId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Trade not found with id: " + tradeId
            );
        }

        return tradeConfirmationRepository
                .findByTrade_TradeId(tradeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Confirmation not found for trade: " + tradeId
                ));
    }
}

