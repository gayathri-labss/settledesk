
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
public class SettlementEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(SettlementEventConsumer.class);

    private static final String CONSUMER_NAME =
            "settlement-consumer-group";

    private static final Set<String> SETTLEMENT_EVENT_TYPES = Set.of(
            "SettlementInstructionCreated",
            "SettlementCompleted",
            "SettlementFailed"
    );

    private final ProcessedEventService processedEventService;
    private final ObjectMapper jsonMapper;

    public SettlementEventConsumer(
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

        if (!SETTLEMENT_EVENT_TYPES.contains(event.eventType())) {
            log.debug(
                    "Ignoring non-settlement event: eventType={}",
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

            case "SettlementInstructionCreated" -> {
                SettlementInstructionCreatedEvent settlementEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                SettlementInstructionCreatedEvent.class
                        );

                processSettlementInstructionCreated(settlementEvent);
            }

            case "SettlementCompleted" -> {
                SettlementCompletedEvent settlementEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                SettlementCompletedEvent.class
                        );

                processSettlementCompleted(settlementEvent);
            }

            case "SettlementFailed" -> {
                SettlementFailedEvent settlementEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                SettlementFailedEvent.class
                        );

                processSettlementFailed(settlementEvent);
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported settlement event type: " + event.eventType()
            );
        }
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

    private void processSettlementCompleted(
            SettlementCompletedEvent event) {

        log.info(
                "Processing SettlementCompletedEvent: settlementId={}, tradeId={}, settlementReference={}, settlementDate={}, status={}, completedAt={}",
                event.settlementId(),
                event.tradeId(),
                event.settlementReference(),
                event.settlementDate(),
                event.status(),
                event.completedAt()
        );
    }

    private void processSettlementFailed(
            SettlementFailedEvent event) {

        log.info(
                "Processing SettlementFailedEvent: settlementId={}, tradeId={}, settlementReference={}, settlementDate={}, failureReason={}, status={}, failedAt={}",
                event.settlementId(),
                event.tradeId(),
                event.settlementReference(),
                event.settlementDate(),
                event.failureReason(),
                event.status(),
                event.failedAt()
        );
    }
}
