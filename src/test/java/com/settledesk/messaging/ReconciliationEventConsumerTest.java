
package com.settledesk.messaging;

import com.settledesk.event.BusinessEvent;
import com.settledesk.event.ReconciliationBreakDetectedEvent;
import com.settledesk.event.ReconciliationCompletedEvent;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationEventConsumerTest {

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private ObjectMapper objectMapper;

    private ReconciliationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ReconciliationEventConsumer(
                processedEventService,
                objectMapper
        );
    }

    private BusinessEvent createEvent(String eventType) {
        return new BusinessEvent(
                UUID.randomUUID(),
                eventType,
                "RECONCILIATION",
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
        BusinessEvent event = createEvent("TradeCreated");

        consumer.consumeBusinessEvent(event);

        verifyNoInteractions(processedEventService, objectMapper);
    }

    @Test
    void consumeBusinessEvent_shouldIgnoreDuplicateEvent() {
        BusinessEvent event = createEvent("ReconciliationCompleted");

        when(processedEventService.markProcessedIfNew(
                "reconciliation-consumer-group",
                event.eventId()
        )).thenReturn(false);

        consumer.consumeBusinessEvent(event);

        verify(processedEventService).markProcessedIfNew(
                "reconciliation-consumer-group",
                event.eventId()
        );
        verifyNoInteractions(objectMapper);
    }

    @Test
    void consumeBusinessEvent_shouldProcessReconciliationCompleted()
            throws Exception {
        BusinessEvent event = createEvent("ReconciliationCompleted");

        ReconciliationCompletedEvent payload =
                mock(ReconciliationCompletedEvent.class);

        when(processedEventService.markProcessedIfNew(
                "reconciliation-consumer-group",
                event.eventId()
        )).thenReturn(true);

        when(objectMapper.treeToValue(
                event.payload(),
                ReconciliationCompletedEvent.class
        )).thenReturn(payload);

        consumer.consumeBusinessEvent(event);

        verify(objectMapper).treeToValue(
                event.payload(),
                ReconciliationCompletedEvent.class
        );
    }

    @Test
    void consumeBusinessEvent_shouldProcessBreakDetected()
            throws Exception {
        BusinessEvent event = createEvent("ReconciliationBreakDetected");

        ReconciliationBreakDetectedEvent payload =
                mock(ReconciliationBreakDetectedEvent.class);

        when(processedEventService.markProcessedIfNew(
                "reconciliation-consumer-group",
                event.eventId()
        )).thenReturn(true);

        when(objectMapper.treeToValue(
                event.payload(),
                ReconciliationBreakDetectedEvent.class
        )).thenReturn(payload);

        consumer.consumeBusinessEvent(event);

        verify(objectMapper).treeToValue(
                event.payload(),
                ReconciliationBreakDetectedEvent.class
        );
    }
}
