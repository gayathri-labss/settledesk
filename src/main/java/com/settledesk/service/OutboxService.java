package com.settledesk.service;

import com.settledesk.event.TradeCreatedEvent;
import com.settledesk.model.OutboxEvent;
import com.settledesk.model.OutboxStatus;
import com.settledesk.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

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
    public OutboxEvent saveTradeCreatedEvent(TradeCreatedEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(event.eventId())
                .aggregateType("TRADE")
                .aggregateId(event.tradeId().toString())
                .eventType("TradeCreated")
                .payload(jsonMapper.writeValueAsString(event))
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        return outboxEventRepository.save(outboxEvent);
    }
}