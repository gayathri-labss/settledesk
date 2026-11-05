
package com.settledesk.messaging;

import com.settledesk.event.TradeCreatedEvent;
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

    @KafkaListener(topics = "trade-events", containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTradeCreated(TradeCreatedEvent event) {

        if (event == null) {
            log.error("Received null TradeCreatedEvent. Skipping event.");
            return;
        }

        if (event.eventId() == null) {
            log.error("Received TradeCreatedEvent with null eventId. Skipping event.");
            return;
        }

        boolean isNew = processedEventService.markProcessedIfNew(event.eventId());

        if (!isNew) {
            log.info(
                    "Duplicate TradeCreatedEvent skipped: eventId={}",
                    event.eventId()
            );
            return;
        }

        log.info(
                "TradeCreatedEvent receipt recorded: eventId={}, tradeId={}",
                event.eventId(),
                event.tradeId()
        );
    }
}
