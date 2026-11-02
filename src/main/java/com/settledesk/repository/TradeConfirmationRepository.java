package com.settledesk.repository;

import com.settledesk.model.TradeConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeConfirmationRepository
        extends JpaRepository<TradeConfirmation, Long> {

    Optional<TradeConfirmation> findByTrade_TradeId(Long tradeId);

    boolean existsByTrade_TradeId(Long tradeId);
}