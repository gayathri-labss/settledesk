package com.settledesk.repository;

import com.settledesk.model.SettlementInstruction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository
        extends JpaRepository<SettlementInstruction, Long> {

    Optional<SettlementInstruction> findByTrade_TradeId(Long tradeId);
    List<SettlementInstruction> findBySettlementDate(LocalDate settlementDate);

    boolean existsByTrade_TradeId(Long tradeId);
}