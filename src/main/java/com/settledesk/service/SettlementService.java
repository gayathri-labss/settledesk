package com.settledesk.service;

import com.settledesk.dto.CreateSettlementRequest;
import com.settledesk.dto.UpdateTradeStatusRequest;
import com.settledesk.model.SettlementInstruction;
import com.settledesk.model.SettlementStatus;
import com.settledesk.model.Trade;
import com.settledesk.model.TradeStatus;
import com.settledesk.repository.SettlementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final TradeService tradeService;

    public SettlementService(
            SettlementRepository settlementRepository,
            TradeService tradeService) {

        this.settlementRepository = settlementRepository;
        this.tradeService = tradeService;
    }

    @Transactional
    public SettlementInstruction createSettlementInstruction(
            CreateSettlementRequest request) {

        Trade trade = tradeService.getTradeByTradeId(
                request.getTradeId()
        );

        if (trade.getStatus() != TradeStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Settlement can only be created for confirmed trades"
            );
        }

        if (settlementRepository.existsByTrade_TradeId(
                trade.getTradeId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Settlement instruction already exists for trade: "
                            + trade.getTradeId()
            );
        }

        SettlementInstruction instruction =
                SettlementInstruction.builder()
                        .trade(trade)
                        .settlementReference(
                                "SET-" + UUID.randomUUID()
                        )
                        .settlementDate(request.getSettlementDate())
                        .status(SettlementStatus.CREATED)
                        .createdAt(LocalDateTime.now())
                        .build();

        return settlementRepository.save(instruction);
    }

    @Transactional(readOnly = true)
    public List<SettlementInstruction> getAllSettlements() {
        return settlementRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SettlementInstruction getSettlementById(Long settlementId) {

        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Settlement not found with ID: " + settlementId
                ));
    }

    @Transactional
    public SettlementInstruction submitSettlement(Long settlementId) {

        SettlementInstruction instruction =
                getSettlementById(settlementId);

        if (instruction.getStatus() != SettlementStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only CREATED settlements can be submitted"
            );
        }

        Trade trade = instruction.getTrade();

        UpdateTradeStatusRequest statusRequest =
                new UpdateTradeStatusRequest();

        statusRequest.setStatus(TradeStatus.SETTLEMENT_PENDING);

        tradeService.updateTradeStatus(
                trade.getTradeId(),
                statusRequest
        );

        instruction.setStatus(SettlementStatus.SUBMITTED);
        instruction.setSubmittedAt(LocalDateTime.now());

        return settlementRepository.save(instruction);
    }
}