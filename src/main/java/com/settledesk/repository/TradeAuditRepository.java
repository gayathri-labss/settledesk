package com.settledesk.repository;

import com.settledesk.model.TradeAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeAuditRepository
        extends JpaRepository<TradeAuditEvent, Long> {

    List<TradeAuditEvent>
    findByTrade_TradeIdOrderByPerformedAtAsc(Long tradeId);
}