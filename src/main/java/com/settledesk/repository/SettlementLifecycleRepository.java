package com.settledesk.repository;

import com.settledesk.model.SettlementLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementLifecycleRepository
        extends JpaRepository<SettlementLifecycleEvent, Long> {

    List<SettlementLifecycleEvent>
    findBySettlement_SettlementIdOrderByChangedAtAsc(Long settlementId);
}