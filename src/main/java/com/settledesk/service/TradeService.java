package com.settledesk.service;

import com.settledesk.dto.CreateTradeRequest;
import com.settledesk.dto.UpdateTradeRequest;
import com.settledesk.model.Trade;
import com.settledesk.model.TradeStatus;
import org.springframework.stereotype.Service;
import com.settledesk.repository.TradeRepository;
import com.settledesk.specification.TradeSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import com.settledesk.dto.UpdateTradeStatusRequest;
import java.util.List;

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
                .status(TradeStatus.NEW)
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
    public List<Trade> createTrades(List<CreateTradeRequest> requests) {

        List<Trade> trades = requests.stream()
                .map(request -> Trade.builder()
                        .instrument(request.getInstrument())
                        .quantity(request.getQuantity())
                        .price(request.getPrice())
                        .buyer(request.getBuyer())
                        .seller(request.getSeller())
                        .status(TradeStatus.NEW)
                        .build())
                .toList();

        return tradeRepository.saveAll(trades);
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

    private boolean isValidStatusTransition(
            TradeStatus currentStatus,
            TradeStatus newStatus) {

        if (currentStatus == null || newStatus == null) {
            return false;
        }

        return switch (currentStatus) {
            case NEW ->
                    newStatus == TradeStatus.MATCHED ||
                            newStatus == TradeStatus.CANCELLED;

            case MATCHED ->
                    newStatus == TradeStatus.CONFIRMED ||
                            newStatus == TradeStatus.FAILED ||
                            newStatus == TradeStatus.CANCELLED;

            case CONFIRMED ->
                    newStatus == TradeStatus.SETTLEMENT_PENDING ||
                            newStatus == TradeStatus.FAILED;

            case SETTLEMENT_PENDING ->
                    newStatus == TradeStatus.SETTLED ||
                            newStatus == TradeStatus.FAILED;

            case SETTLED, FAILED, CANCELLED -> false;
        };
    }

    @Transactional
    public Trade updateTradeStatus(
            Long tradeId,
            UpdateTradeStatusRequest request) {

        Trade existingTrade = getTradeByTradeId(tradeId);

        TradeStatus currentStatus = existingTrade.getStatus();
        TradeStatus newStatus = request.getStatus();

        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid trade status transition from "
                            + currentStatus + " to " + newStatus
            );
        }

        existingTrade.setStatus(newStatus);

        return tradeRepository.save(existingTrade);
    }
}