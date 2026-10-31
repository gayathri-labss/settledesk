package com.settledesk.repository;

import com.settledesk.model.TradeLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeLifecycleRepository
        extends JpaRepository<TradeLifecycleEvent, Long> {

    List<TradeLifecycleEvent>
    findByTrade_TradeIdOrderByChangedAtAsc(Long tradeId);
}