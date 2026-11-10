
package com.settledesk.messaging;

import com.settledesk.event.BusinessEvent;
import com.settledesk.model.OutboxEvent;
import com.settledesk.model.OutboxStatus;
import com.settledesk.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private BusinessEventProducer businessEventProducer;

    private JsonMapper jsonMapper;
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        outboxPublisher = new OutboxPublisher(
                outboxEventRepository,
                businessEventProducer,
                jsonMapper
        );
    }

    private OutboxEvent createPendingEvent() throws Exception {
        BusinessEvent event = new BusinessEvent(
                UUID.randomUUID(),
                "TradeCreated",
                "TRADE",
                "1001",
                Instant.now(),
                1,
                jsonMapper.createObjectNode().put("tradeId", 1001)
        );

        return OutboxEvent.builder()
                .eventId(event.eventId())
                .aggregateType(event.aggregateType())
                .aggregateId(event.aggregateId())
                .eventType(event.eventType())
                .payload(jsonMapper.writeValueAsString(event))
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void publishPendingEvents_shouldPublishAndMarkEventAsPublished()
            throws Exception {

        OutboxEvent event = createPendingEvent();

        when(outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        when(businessEventProducer.publishBusinessEvent(any(BusinessEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishPendingEvents();

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());

        verify(businessEventProducer, times(1))
                .publishBusinessEvent(any(BusinessEvent.class));

        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    void publishPendingEvents_whenKafkaFails_shouldKeepEventPending()
            throws Exception {

        OutboxEvent event = createPendingEvent();

        when(outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        when(businessEventProducer.publishBusinessEvent(any(BusinessEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka unavailable")
                ));

        outboxPublisher.publishPendingEvents();

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertNull(event.getPublishedAt());

        verify(businessEventProducer, times(1))
                .publishBusinessEvent(any(BusinessEvent.class));

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void publishPendingEvents_withInvalidPayload_shouldNotPublish()
            throws Exception {

        OutboxEvent event = createPendingEvent();
        event.setPayload("invalid-json");

        when(outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        outboxPublisher.publishPendingEvents();

        verify(businessEventProducer, never())
                .publishBusinessEvent(any(BusinessEvent.class));

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));

        assertEquals(OutboxStatus.PENDING, event.getStatus());
    }
}
