package com.settledesk.service;

import com.settledesk.dto.MatchingRunRequest;
import com.settledesk.dto.UpdateTradeStatusRequest;

import com.settledesk.model.MatchingOutcome;
import com.settledesk.model.MatchingResult;
import com.settledesk.model.MatchingRun;
import com.settledesk.model.MatchingRunStatus;
import com.settledesk.model.Trade;
import com.settledesk.model.TradeConfirmation;
import com.settledesk.model.TradeStatus;

import com.settledesk.repository.MatchingRunRepository;
import com.settledesk.repository.MatchingResultRepository;
import com.settledesk.repository.TradeRepository;
import com.settledesk.repository.TradeConfirmationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class MatchingService {

    private final MatchingRunRepository matchingRunRepository;
    private final MatchingResultRepository matchingResultRepository;
    private final TradeRepository tradeRepository;
    private final TradeConfirmationRepository tradeConfirmationRepository;
    private final TradeService tradeService;

    public MatchingService(
            MatchingRunRepository matchingRunRepository,
            MatchingResultRepository matchingResultRepository,
            TradeRepository tradeRepository,
            TradeConfirmationRepository tradeConfirmationRepository,
            TradeService tradeService) {

        this.matchingRunRepository = matchingRunRepository;
        this.matchingResultRepository = matchingResultRepository;
        this.tradeRepository = tradeRepository;
        this.tradeConfirmationRepository = tradeConfirmationRepository;
        this.tradeService = tradeService;
    }

    @Transactional
    public MatchingRun startMatchingRun(MatchingRunRequest request) {

        List<Long> tradeIds = request.getTradeIds();

        if (new HashSet<>(tradeIds).size() != tradeIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate trade IDs are not allowed"
            );
        }

        List<Trade> trades = tradeIds.stream()
                .map(tradeId -> tradeRepository.findById(tradeId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Trade not found with id: " + tradeId
                        )))
                .toList();

        MatchingRun matchingRun = MatchingRun.builder()
                .status(MatchingRunStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .totalTrades(trades.size())
                .matchedCount(0)
                .unmatchedCount(0)
                .build();

        MatchingRun savedRun = matchingRunRepository.save(matchingRun);

        for (Trade trade : trades) {

            TradeConfirmation confirmation =
                    tradeConfirmationRepository
                            .findByTrade_TradeId(trade.getTradeId())
                            .orElse(null);

            List<String> mismatches = new ArrayList<>();

            if (confirmation == null) {
                mismatches.add("No counterparty confirmation found");
            } else {
                compareTradeDetails(trade, confirmation, mismatches);
            }

            MatchingOutcome outcome = mismatches.isEmpty()
                    ? MatchingOutcome.MATCHED
                    : MatchingOutcome.UNMATCHED;

            MatchingResult result = MatchingResult.builder()
                    .matchingRun(savedRun)
                    .trade(trade)
                    .outcome(outcome)
                    .mismatchDetails(
                            mismatches.isEmpty()
                                    ? null
                                    : String.join("; ", mismatches)
                    )
                    .evaluatedAt(LocalDateTime.now())
                    .build();

            matchingResultRepository.save(result);

            if (outcome == MatchingOutcome.MATCHED) {

                savedRun.setMatchedCount(
                        savedRun.getMatchedCount() + 1
                );

                if (trade.getStatus() == TradeStatus.NEW) {

                    UpdateTradeStatusRequest statusRequest =
                            new UpdateTradeStatusRequest();

                    statusRequest.setStatus(TradeStatus.MATCHED);

                    tradeService.updateTradeStatus(
                            trade.getTradeId(),
                            statusRequest
                    );
                }

            } else {

                savedRun.setUnmatchedCount(
                        savedRun.getUnmatchedCount() + 1
                );
            }
        }

        savedRun.setStatus(MatchingRunStatus.COMPLETED);
        savedRun.setCompletedAt(LocalDateTime.now());

        return matchingRunRepository.save(savedRun);
    }

    private void compareTradeDetails(
            Trade trade,
            TradeConfirmation confirmation,
            List<String> mismatches) {

        if (!Objects.equals(
                trade.getInstrument(),
                confirmation.getInstrument())) {

            mismatches.add(
                    "Instrument mismatch: Internal = "
                            + trade.getInstrument()
                            + ", Counterparty = "
                            + confirmation.getInstrument()
            );
        }

        if (!sameAmount(
                trade.getQuantity(),
                confirmation.getQuantity())) {

            mismatches.add(
                    "Quantity mismatch: Internal = "
                            + trade.getQuantity()
                            + ", Counterparty = "
                            + confirmation.getQuantity()
            );
        }

        if (!sameAmount(
                trade.getPrice(),
                confirmation.getPrice())) {

            mismatches.add(
                    "Price mismatch: Internal = "
                            + trade.getPrice()
                            + ", Counterparty = "
                            + confirmation.getPrice()
            );
        }

        if (!Objects.equals(
                trade.getBuyer(),
                confirmation.getBuyer())) {

            mismatches.add(
                    "Buyer mismatch: Internal = "
                            + trade.getBuyer()
                            + ", Counterparty = "
                            + confirmation.getBuyer()
            );
        }

        if (!Objects.equals(
                trade.getSeller(),
                confirmation.getSeller())) {

            mismatches.add(
                    "Seller mismatch: Internal = "
                            + trade.getSeller()
                            + ", Counterparty = "
                            + confirmation.getSeller()
            );
        }
    }

    private boolean sameAmount(BigDecimal internalValue,
                               BigDecimal confirmationValue) {

        return internalValue != null
                && confirmationValue != null
                && internalValue.compareTo(confirmationValue) == 0;
    }

    public List<MatchingRun> getAllMatchingRuns() {
        return matchingRunRepository.findAll();
    }

    public MatchingRun getMatchingRunById(Long runId) {
        return matchingRunRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Matching run not found with ID: " + runId
                ));
    }

    @Transactional(readOnly = true)
    public List<MatchingResult> getMatchingResultsByRunId(Long runId) {

        matchingRunRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Matching run not found with ID: " + runId
                ));

        return matchingResultRepository
                .findByMatchingRun_RunIdOrderByEvaluatedAtAsc(runId);
    }

    @Transactional(readOnly = true)
    public List<MatchingResult> getMatchingExceptions() {

        return matchingResultRepository
                .findByOutcomeOrderByEvaluatedAtAsc(MatchingOutcome.UNMATCHED);
    }
}