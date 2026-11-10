
package com.settledesk.messaging;

import com.settledesk.event.BusinessEvent;
import com.settledesk.event.ReconciliationBreakDetectedEvent;
import com.settledesk.event.ReconciliationCompletedEvent;
import com.settledesk.service.ProcessedEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ReconciliationEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(ReconciliationEventConsumer.class);

    private static final String CONSUMER_NAME =
            "reconciliation-consumer-group";

    private final ProcessedEventService processedEventService;
    private final ObjectMapper jsonMapper;

    public ReconciliationEventConsumer(
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

        if (!"ReconciliationCompleted".equals(event.eventType()) && !"ReconciliationBreakDetected".equals(event.eventType())) {

            log.debug(
                    "Ignoring non-reconciliation event: eventType={}",
                    event.eventType()
            );
            return;
        }

        boolean isNew = processedEventService.markProcessedIfNew(
                CONSUMER_NAME,
                event.eventId()
        );

        if (!isNew) {
            log.info(
                    "Duplicate reconciliation event detected: eventId={}, eventType={}",
                    event.eventId(),
                    event.eventType()
            );
            return;
        }

        switch (event.eventType()) {

            case "ReconciliationCompleted" -> {
                ReconciliationCompletedEvent reconciliationEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                ReconciliationCompletedEvent.class
                        );

                processReconciliationCompleted(reconciliationEvent);
            }

            case "ReconciliationBreakDetected" -> {
                ReconciliationBreakDetectedEvent breakEvent =
                        jsonMapper.treeToValue(
                                event.payload(),
                                ReconciliationBreakDetectedEvent.class
                        );

                processReconciliationBreakDetected(breakEvent);
            }

            default -> throw new IllegalStateException(
                    "Unexpected reconciliation event type: "
                            + event.eventType()
            );
        }
    }

    private void processReconciliationCompleted(
            ReconciliationCompletedEvent event) {

        log.info(
                "Processing ReconciliationCompletedEvent: runId={}, source={}, status={}, totalInternalRecords={}, totalExternalRecords={}, matchedCount={}, mismatchedCount={}, missingExternalCount={}, missingInternalCount={}, completedAt={}",
                event.runId(),
                event.source(),
                event.status(),
                event.totalInternalRecords(),
                event.totalExternalRecords(),
                event.matchedCount(),
                event.mismatchedCount(),
                event.missingExternalCount(),
                event.missingInternalCount(),
                event.completedAt()
        );
    }

    private void processReconciliationBreakDetected(
            ReconciliationBreakDetectedEvent event) {

        log.info(
                "Processing ReconciliationBreakDetectedEvent: breakId={}, runId={}, category={}, status={}, priority={}, internalSettlementReference={}, externalReference={}, externalSettlementReference={}, detectedAt={}",
                event.breakId(),
                event.runId(),
                event.category(),
                event.status(),
                event.priority(),
                event.internalSettlementReference(),
                event.externalReference(),
                event.externalSettlementReference(),
                event.detectedAt()
        );
    }
}
