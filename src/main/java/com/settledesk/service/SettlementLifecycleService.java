package com.settledesk.service;

import com.settledesk.model.SettlementInstruction;
import com.settledesk.model.SettlementLifecycleEvent;
import com.settledesk.model.SettlementStatus;
import com.settledesk.repository.SettlementLifecycleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SettlementLifecycleService {

    private final SettlementLifecycleRepository lifecycleRepository;

    public SettlementLifecycleService(
            SettlementLifecycleRepository lifecycleRepository) {
        this.lifecycleRepository = lifecycleRepository;
    }

    @Transactional
    public void recordStatusChange(
            SettlementInstruction settlement,
            SettlementStatus previousStatus,
            SettlementStatus newStatus) {

        SettlementLifecycleEvent event =
                SettlementLifecycleEvent.builder()
                        .settlement(settlement)
                        .previousStatus(previousStatus)
                        .newStatus(newStatus)
                        .changedAt(LocalDateTime.now())
                        .build();

        lifecycleRepository.save(event);
    }
}