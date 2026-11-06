
package com.settledesk.messaging;

import com.settledesk.event.*;
import com.settledesk.service.ProcessedEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Service
public class TradeEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TradeEventConsumer.class);

    private static final String CONSUMER_NAME =
            "trade-consumer-group";

    private static final Set<String> TRADE_EVENT_TYPES = Set.of(
            "TradeCreated",
            "TradeConfirmed",
            "TradeMatched",
            "TradeMatchingFailed"
    );

    private final ProcessedEventService processedEventService;
    private final ObjectMapper jsonMapper;

    public TradeEventConsumer(
            ProcessedEventService processedEventService,
            ObjectMapper jsonMapper
    ) {
        this.processedEventService = processedEventService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    @KafkaListener(
            topics = "trade-events",
            groupId = CONSUMER_NAME,
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

        if (!TRADE_EVENT_TYPES.contains(event.eventType())) {
            log.debug(
                    "Ignoring non-trade event: eventType={}",
                    event.eventType()
            );
            return;
        }

        boolean isNew =
                processedEventService.markProcessedIfNew(
                        CONSUMER_NAME,
                        event.eventId()
                );

        if (!isNew) {
            log.info(
                    "Duplicate business event detected: eventId={}, eventType={}",
                    event.eventId(),
                    event.eventType()
            );
            return;
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

            case "TradeMatchingFailed" -> {
                TradeMatchingFailedEvent tradeMatchingFailedEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                TradeMatchingFailedEvent.class
                        );

                processTradeMatchingFailed(tradeMatchingFailedEvent);
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported trade event type: " + event.eventType()
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

    private void processTradeMatchingFailed(
            TradeMatchingFailedEvent event) {

        log.info(
                "Processing TradeMatchingFailedEvent: tradeId={}, matchingRunId={}, failureReason={}, failedAt={}",
                event.tradeId(),
                event.matchingRunId(),
                event.failureReason(),
                event.failedAt()
        );
    }
}