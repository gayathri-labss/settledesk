
package com.settledesk.messaging;

import com.settledesk.event.*;
import com.settledesk.service.ProcessedEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementEventConsumerTest {

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private ObjectMapper objectMapper;

    private SettlementEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SettlementEventConsumer(
                processedEventService,
                objectMapper
        );
    }

    private BusinessEvent createEvent(String eventType) {
        return new BusinessEvent(
                UUID.randomUUID(),
                eventType,
                "SETTLEMENT",
                "1001",
                Instant.now(),
                1,
                JsonMapper.builder().build().createObjectNode()
        );
    }

    @Test
    void consumeBusinessEvent_shouldRejectNullEvent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> consumer.consumeBusinessEvent(null)
        );
        verifyNoInteractions(processedEventService, objectMapper);
    }

    @Test
    void consumeBusinessEvent_shouldIgnoreUnrelatedEvent() {
        consumer.consumeBusinessEvent(createEvent("TradeCreated"));
        verifyNoInteractions(processedEventService, objectMapper);
    }

    @Test
    void consumeBusinessEvent_shouldIgnoreDuplicateEvent() {
        BusinessEvent event = createEvent("SettlementCompleted");

        when(processedEventService.markProcessedIfNew(
                "settlement-consumer-group", event.eventId()
        )).thenReturn(false);

        consumer.consumeBusinessEvent(event);

        verify(processedEventService).markProcessedIfNew(
                "settlement-consumer-group", event.eventId()
        );
        verifyNoInteractions(objectMapper);
    }

    @Test
    void consumeBusinessEvent_shouldProcessInstructionCreated()
            throws Exception {
        BusinessEvent event = createEvent("SettlementInstructionCreated");

        when(processedEventService.markProcessedIfNew(
                "settlement-consumer-group", event.eventId()
        )).thenReturn(true);

        when(objectMapper.treeToValue(
                event.payload(), SettlementInstructionCreatedEvent.class
        )).thenReturn(mock(SettlementInstructionCreatedEvent.class));

        consumer.consumeBusinessEvent(event);

        verify(objectMapper).treeToValue(
                event.payload(), SettlementInstructionCreatedEvent.class
        );
    }

    @Test
    void consumeBusinessEvent_shouldProcessSettlementCompleted()
            throws Exception {
        BusinessEvent event = createEvent("SettlementCompleted");

        when(processedEventService.markProcessedIfNew(
                "settlement-consumer-group", event.eventId()
        )).thenReturn(true);

        when(objectMapper.treeToValue(
                event.payload(), SettlementCompletedEvent.class
        )).thenReturn(mock(SettlementCompletedEvent.class));

        consumer.consumeBusinessEvent(event);

        verify(objectMapper).treeToValue(
                event.payload(), SettlementCompletedEvent.class
        );
    }

    @Test
    void consumeBusinessEvent_shouldProcessSettlementFailed()
            throws Exception {
        BusinessEvent event = createEvent("SettlementFailed");

        when(processedEventService.markProcessedIfNew(
                "settlement-consumer-group", event.eventId()
        )).thenReturn(true);

        when(objectMapper.treeToValue(
                event.payload(), SettlementFailedEvent.class
        )).thenReturn(mock(SettlementFailedEvent.class));

        consumer.consumeBusinessEvent(event);

        verify(objectMapper).treeToValue(
                event.payload(), SettlementFailedEvent.class
        );
    }
}
