package com.settledesk.service;

import com.settledesk.dto.AuditEventResponse;
import com.settledesk.model.TradeAuditEvent;
import com.settledesk.model.SettlementAuditEvent;
import com.settledesk.repository.TradeAuditRepository;
import com.settledesk.repository.SettlementAuditRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final TradeAuditRepository tradeAuditRepository;
    private final SettlementAuditRepository settlementAuditRepository;

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAllAuditEvents() {

        List<AuditEventResponse> tradeEvents =
                tradeAuditRepository.findAll()
                        .stream()
                        .map(this::mapTradeAudit)
                        .toList();

        List<AuditEventResponse> settlementEvents =
                settlementAuditRepository.findAll()
                        .stream()
                        .map(this::mapSettlementAudit)
                        .toList();

        return Stream.concat(
                        tradeEvents.stream(),
                        settlementEvents.stream()
                )
                .sorted(Comparator.comparing(
                        AuditEventResponse::getPerformedAt
                ).reversed())
                .toList();
    }

    private AuditEventResponse mapTradeAudit(TradeAuditEvent event) {

        return AuditEventResponse.builder()
                .auditId(event.getAuditId())
                .eventType("TRADE")
                .entityId(event.getTrade().getTradeId())
                .action(event.getAction().name())
                .details(event.getDetails())
                .performedAt(event.getPerformedAt())
                .build();
    }

    private AuditEventResponse mapSettlementAudit(
            SettlementAuditEvent event) {

        return AuditEventResponse.builder()
                .auditId(event.getAuditId())
                .eventType("SETTLEMENT")
                .entityId(event.getSettlement().getSettlementId())
                .action(event.getAction().name())
                .details(event.getDetails())
                .performedAt(event.getPerformedAt())
                .build();
    }
}