package com.settledesk.service;

import com.settledesk.dto.CreateTradeRequest;
import com.settledesk.dto.UpdateTradeRequest;
import com.settledesk.model.Trade;
import org.springframework.stereotype.Service;
import com.settledesk.repository.TradeRepository;
import com.settledesk.specification.TradeSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public Trade createTrade(CreateTradeRequest request) {

        return tradeRepository.save(Trade.builder()
                .instrument(request.getInstrument())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .buyer(request.getBuyer())
                .seller(request.getSeller())
                .build());
    }

    public Trade getTradeByTradeId(Long tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trade not found with id: " + tradeId
                ));
    }

    @Transactional
    public Trade updateTrade(Long tradeId, UpdateTradeRequest request) {

        Trade existingTrade = getTradeByTradeId(tradeId);

        existingTrade.setInstrument(request.getInstrument());
        existingTrade.setQuantity(request.getQuantity());
        existingTrade.setPrice(request.getPrice());
        existingTrade.setBuyer(request.getBuyer());
        existingTrade.setSeller(request.getSeller());

        return tradeRepository.save(existingTrade);
    }

    public Page<Trade> searchTrades(
            String instrument,
            String buyer,
            Pageable pageable) {

        Specification<Trade> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (instrument != null && !instrument.isBlank()) {
            specification = specification.and(
                    TradeSpecification.hasInstrument(instrument));
        }

        if (buyer != null && !buyer.isBlank()) {
            specification = specification.and(
                    TradeSpecification.hasBuyer(buyer));
        }

        return tradeRepository.findAll(specification, pageable);
    }
}