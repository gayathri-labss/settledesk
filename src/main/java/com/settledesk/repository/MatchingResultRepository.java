package com.settledesk.repository;

import com.settledesk.model.MatchingResult;
import com.settledesk.model.MatchingOutcome;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchingResultRepository
        extends JpaRepository<MatchingResult, Long> {

    List<MatchingResult>
    findByMatchingRun_RunIdOrderByEvaluatedAtAsc(Long runId);

    List<MatchingResult>
    findByOutcomeOrderByEvaluatedAtAsc(MatchingOutcome outcome);

    boolean existsByMatchingRun_RunIdAndTrade_TradeId(
            Long runId,
            Long tradeId
    );
}