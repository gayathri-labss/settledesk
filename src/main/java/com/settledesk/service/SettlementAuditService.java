package com.settledesk.service;

import com.settledesk.model.SettlementInstruction;
import com.settledesk.model.SettlementAuditAction;
import com.settledesk.model.SettlementAuditEvent;
import com.settledesk.repository.SettlementAuditRepository;
import com.settledesk.repository.SettlementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettlementAuditService {

    private final SettlementAuditRepository auditRepository;
    private final SettlementRepository settlementRepository;

    public SettlementAuditService(
            SettlementAuditRepository auditRepository,
            SettlementRepository settlementRepository) {

        this.auditRepository = auditRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public void recordAudit(
            SettlementInstruction settlement,
            SettlementAuditAction action,
            String details) {

        SettlementAuditEvent event = SettlementAuditEvent.builder()
                .settlement(settlement)
                .action(action)
                .details(details)
                .performedAt(LocalDateTime.now())
                .build();

        auditRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<SettlementAuditEvent> getSettlementAuditHistory(
            Long settlementId) {

        if (!settlementRepository.existsById(settlementId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Settlement not found with ID: " + settlementId
            );
        }

        return auditRepository
                .findBySettlement_SettlementIdOrderByPerformedAtAsc(
                        settlementId
                );
    }
}