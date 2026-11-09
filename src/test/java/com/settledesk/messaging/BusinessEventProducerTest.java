
package com.settledesk.messaging;

import com.settledesk.event.BusinessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessEventProducerTest {

    @Mock
    private KafkaTemplate<String, BusinessEvent> kafkaTemplate;

    private BusinessEventProducer producer;
    private BusinessEvent event;

    @BeforeEach
    void setUp() {
        producer = new BusinessEventProducer(kafkaTemplate);

        event = new BusinessEvent(
                UUID.randomUUID(),
                "TradeCreated",
                "TRADE",
                "1001",
                Instant.now(),
                1,
                JsonMapper.builder().build()
                        .createObjectNode()
                        .put("tradeId", 1001)
        );
    }

    @Test
    void publishBusinessEvent_shouldSendEventWithAggregateIdAsKey() {
        CompletableFuture<SendResult<String, BusinessEvent>> future =
                new CompletableFuture<>();

        when(kafkaTemplate.send("trade-events", "1001", event))
                .thenReturn(future);

        CompletableFuture<SendResult<String, BusinessEvent>> result =
                producer.publishBusinessEvent(event);

        assertNotNull(result);
        assertFalse(result.isDone());

        verify(kafkaTemplate, times(1))
                .send("trade-events", "1001", event);
    }

    @Test
    void publishBusinessEvent_withNullEvent_shouldThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> producer.publishBusinessEvent(null)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publishBusinessEvent_withMissingEventId_shouldThrowException() {
        BusinessEvent invalidEvent = new BusinessEvent(
                null,
                "TradeCreated",
                "TRADE",
                "1001",
                Instant.now(),
                1,
                event.payload()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> producer.publishBusinessEvent(invalidEvent)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publishBusinessEvent_withMissingAggregateId_shouldThrowException() {
        BusinessEvent invalidEvent = new BusinessEvent(
                UUID.randomUUID(),
                "TradeCreated",
                "TRADE",
                null,
                Instant.now(),
                1,
                event.payload()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> producer.publishBusinessEvent(invalidEvent)
        );

        verifyNoInteractions(kafkaTemplate);
    }
}
