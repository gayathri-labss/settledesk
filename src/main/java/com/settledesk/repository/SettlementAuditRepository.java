package com.settledesk.repository;

import com.settledesk.model.SettlementAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementAuditRepository
        extends JpaRepository<SettlementAuditEvent, Long> {

    List<SettlementAuditEvent>
    findBySettlement_SettlementIdOrderByPerformedAtAsc(Long settlementId);
}