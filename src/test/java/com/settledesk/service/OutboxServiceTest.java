
package com.settledesk.service;

import com.settledesk.event.BusinessEvent;
import com.settledesk.model.OutboxEvent;
import com.settledesk.model.OutboxStatus;
import com.settledesk.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxService outboxService;

    private JsonMapper jsonMapper;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        outboxService = new OutboxService(
                outboxEventRepository,
                jsonMapper
        );
        eventId = UUID.randomUUID();
    }

    @Test
    void saveBusinessEvent_shouldPersistPendingEvent() {
        BusinessEvent event = new BusinessEvent(
                eventId,
                "TradeCreated",
                "TRADE",
                "1001",
                Instant.now(),
                1,
                jsonMapper.createObjectNode().put("tradeId", 1001)
        );

        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent result = outboxService.saveBusinessEvent(event);

        assertNotNull(result);
        assertEquals(eventId, result.getEventId());
        assertEquals("TRADE", result.getAggregateType());
        assertEquals("1001", result.getAggregateId());
        assertEquals("TradeCreated", result.getEventType());
        assertEquals(OutboxStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNull(result.getPublishedAt());

        assertTrue(result.getPayload().contains("TradeCreated"));
        assertTrue(result.getPayload().contains("1001"));

        verify(outboxEventRepository, times(1))
                .save(any(OutboxEvent.class));
    }

    @Test
    void saveBusinessEvent_shouldSerializeCompleteEventPayload() {
        BusinessEvent event = new BusinessEvent(
                eventId,
                "TradeCreated",
                "TRADE",
                "1001",
                Instant.parse("2026-11-09T10:00:00Z"),
                1,
                jsonMapper.createObjectNode()
                        .put("tradeId", 1001)
                        .put("instrument", "NIFTY")
        );

        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent result = outboxService.saveBusinessEvent(event);

        BusinessEvent restored = jsonMapper.readValue(
                result.getPayload(),
                BusinessEvent.class
        );

        assertEquals(event.eventId(), restored.eventId());
        assertEquals(event.eventType(), restored.eventType());
        assertEquals(event.aggregateType(), restored.aggregateType());
        assertEquals(event.aggregateId(), restored.aggregateId());
        assertEquals(event.version(), restored.version());
        assertEquals(
                event.payload().get("instrument").asString(),
                restored.payload().get("instrument").asString()
        );
    }
}
