package com.settledesk.messaging;

import com.settledesk.event.BusinessEvent;
import com.settledesk.event.TradeCreatedEvent;
import com.settledesk.event.TradeConfirmedEvent;
import com.settledesk.event.TradeMatchedEvent;
import com.settledesk.service.ProcessedEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class TradeEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TradeEventConsumer.class);

    private final ProcessedEventService processedEventService;
    private final ObjectMapper jsonMapper;

    public TradeEventConsumer(
            ProcessedEventService processedEventService,
            ObjectMapper jsonMapper
    ) {
        this.processedEventService = processedEventService;
        this.jsonMapper = jsonMapper;
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

        if (event.payload() == null) {
            throw new IllegalArgumentException(
                    "BusinessEvent payload cannot be null"
            );
        }

        switch (event.eventType()) {

            case "TradeCreated" -> {
                TradeCreatedEvent tradeCreatedEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                TradeCreatedEvent.class
                        );

                processTradeCreated(tradeCreatedEvent);
            }

            case "TradeConfirmed" -> {
                TradeConfirmedEvent tradeConfirmedEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                TradeConfirmedEvent.class
                        );

                processTradeConfirmed(tradeConfirmedEvent);
            }

            case "TradeMatched" -> {
                TradeMatchedEvent tradeMatchedEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                TradeMatchedEvent.class
                        );

                processTradeMatched(tradeMatchedEvent);
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported event type: " + event.eventType()
            );
        }

        boolean isNew =
                processedEventService.markProcessedIfNew(event.eventId());

        if (!isNew) {
            log.info(
                    "Duplicate business event detected: eventId={}, eventType={}",
                    event.eventId(),
                    event.eventType()
            );
        }
    }

    private void processTradeCreated(TradeCreatedEvent event) {

        log.info(
                "Processing TradeCreatedEvent: tradeId={}, instrument={}, quantity={}, price={}",
                event.tradeId(),
                event.instrument(),
                event.quantity(),
                event.price()
        );
    }

    private void processTradeConfirmed(TradeConfirmedEvent event) {

        log.info(
                "Processing TradeConfirmedEvent: confirmationId={}, tradeId={}, counterpartyReference={}",
                event.confirmationId(),
                event.tradeId(),
                event.counterpartyReference()
        );
    }

    private void processTradeMatched(TradeMatchedEvent event) {

        log.info(
                "Processing TradeMatchedEvent: tradeId={}, matchingRunId={}, matchedAt={}",
                event.tradeId(),
                event.matchingRunId(),
                event.matchedAt()
        );
    }
}