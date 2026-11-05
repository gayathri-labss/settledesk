
package com.settledesk.messaging;

import com.settledesk.event.BusinessEvent;
import com.settledesk.service.ProcessedEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TradeEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TradeEventConsumer.class);

    private final ProcessedEventService processedEventService;

    public TradeEventConsumer(ProcessedEventService processedEventService) {
        this.processedEventService = processedEventService;
    }

    @KafkaListener(
            topics = "trade-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBusinessEvent(BusinessEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Received null BusinessEvent"
            );
        }

        if (event.eventId() == null) {
            throw new IllegalArgumentException(
                    "BusinessEvent eventId cannot be null"
            );
        }

        if (event.eventType() == null || event.aggregateId() == null) {
            throw new IllegalArgumentException(
                    "BusinessEvent eventType and aggregateId cannot be null"
            );
        }

        boolean isNew =
                processedEventService.markProcessedIfNew(event.eventId());

        if (!isNew) {
            log.info(
                    "Duplicate business event skipped: eventId={}, eventType={}",
                    event.eventId(),
                    event.eventType()
            );
            return;
        }

        log.info(
                "Business event received: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
                event.eventId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId()
        );
    }
}
