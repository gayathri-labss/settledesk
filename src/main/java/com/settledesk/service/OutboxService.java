package com.settledesk.service;

import com.settledesk.event.*;
import com.settledesk.model.OutboxEvent;
import com.settledesk.model.OutboxStatus;
import com.settledesk.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public OutboxEvent saveBusinessEvent(BusinessEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(event.eventId())
                .aggregateType(event.aggregateType())
                .aggregateId(event.aggregateId())
                .eventType(event.eventType())
                .payload(jsonMapper.writeValueAsString(event))
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        return outboxEventRepository.save(outboxEvent);
    }

    @Transactional
    public OutboxEvent saveTradeCreatedEvent(TradeCreatedEvent event) {

        BusinessEvent businessEvent = new BusinessEvent(
                UUID.randomUUID(),
                "TradeCreated",
                "TRADE",
                event.tradeId().toString(),
                Instant.now(),
                1,
                jsonMapper.valueToTree(event)
        );

        return saveBusinessEvent(businessEvent);
    }

    @Transactional
    public OutboxEvent saveTradeConfirmedEvent(TradeConfirmedEvent event) {

        BusinessEvent businessEvent = new BusinessEvent(
                UUID.randomUUID(),
                "TradeConfirmed",
                "TRADE",
                event.tradeId().toString(),
                Instant.now(),
                1,
                jsonMapper.valueToTree(event)
        );

        return saveBusinessEvent(businessEvent);
    }

    @Transactional
    public OutboxEvent saveTradeMatchedEvent(TradeMatchedEvent event) {

        BusinessEvent businessEvent = new BusinessEvent(
                UUID.randomUUID(),
                "TradeMatched",
                "TRADE",
                event.tradeId().toString(),
                Instant.now(),
                1,
                jsonMapper.valueToTree(event)
        );

        return saveBusinessEvent(businessEvent);
    }

    @Transactional
    public OutboxEvent saveTradeMatchingFailedEvent(TradeMatchingFailedEvent event) {
        BusinessEvent businessEvent = new BusinessEvent(
                UUID.randomUUID(),
                "TradeMatchingFailed",
                "TRADE",
                event.tradeId().toString(),
                Instant.now(),
                1,
                jsonMapper.valueToTree(event)
        );

        return saveBusinessEvent(businessEvent);
    }

    @Transactional
    public OutboxEvent saveSettlementInstructionCreatedEvent(
            SettlementInstructionCreatedEvent event) {

        BusinessEvent businessEvent = new BusinessEvent(
                UUID.randomUUID(),
                "SettlementInstructionCreated",
                "SETTLEMENT",
                event.settlementId().toString(),
                Instant.now(),
                1,
                jsonMapper.valueToTree(event)
        );

        return saveBusinessEvent(businessEvent);
    }

    @Transactional
    public OutboxEvent saveSettlementCompletedEvent(
            SettlementCompletedEvent event) {

        BusinessEvent businessEvent = new BusinessEvent(
                UUID.randomUUID(),
                "SettlementCompleted",
                "SETTLEMENT",
                event.settlementId().toString(),
                Instant.now(),
                1,
                jsonMapper.valueToTree(event)
        );

        return saveBusinessEvent(businessEvent);
    }

    @Transactional
    public OutboxEvent saveSettlementFailedEvent(
            SettlementFailedEvent event) {

        BusinessEvent businessEvent = new BusinessEvent(
                UUID.randomUUID(),
                "SettlementFailed",
                "SETTLEMENT",
                event.settlementId().toString(),
                Instant.now(),
                1,
                jsonMapper.valueToTree(event)
        );

        return saveBusinessEvent(businessEvent);
    }
}