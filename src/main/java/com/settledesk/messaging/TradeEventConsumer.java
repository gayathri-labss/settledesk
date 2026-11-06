package com.settledesk.messaging;

import com.settledesk.event.*;
import com.settledesk.service.ProcessedEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
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

        boolean isNew =
                processedEventService.markProcessedIfNew(event.eventId());

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

            case "SettlementInstructionCreated" -> {
                SettlementInstructionCreatedEvent settlementEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                SettlementInstructionCreatedEvent.class
                        );

                if (settlementEvent.settlementId() == null
                        || settlementEvent.tradeId() == null
                        || settlementEvent.settlementReference() == null
                        || settlementEvent.settlementReference().isBlank()
                        || settlementEvent.settlementDate() == null
                        || settlementEvent.status() == null
                        || settlementEvent.createdAt() == null) {

                    throw new IllegalArgumentException(
                            "Invalid SettlementInstructionCreated event payload"
                    );
                }

                processSettlementInstructionCreated(settlementEvent);
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported event type: " + event.eventType()
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

    private void processTradeMatchingFailed(TradeMatchingFailedEvent event) {

        log.info(
                "Processing TradeMatchingFailedEvent: tradeId={}, matchingRunId={}, failureReason={}, failedAt={}",
                event.tradeId(),
                event.matchingRunId(),
                event.failureReason(),
                event.failedAt()
        );
    }

    private void processSettlementInstructionCreated(
            SettlementInstructionCreatedEvent event) {

        log.info(
                "Processing SettlementInstructionCreatedEvent: settlementId={}, tradeId={}, settlementReference={}, settlementDate={}, status={}, createdAt={}",
                event.settlementId(),
                event.tradeId(),
                event.settlementReference(),
                event.settlementDate(),
                event.status(),
                event.createdAt()
        );
    }
}