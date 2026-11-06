
package com.settledesk.messaging;

import com.settledesk.event.BusinessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class BusinessEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(BusinessEventProducer.class);

    private static final String TOPIC = "trade-events";

    private final KafkaTemplate<String, BusinessEvent> kafkaTemplate;

    public BusinessEventProducer(
            KafkaTemplate<String, BusinessEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, BusinessEvent>>
    publishBusinessEvent(BusinessEvent event) {

        if (event == null || event.eventId() == null
                || event.aggregateId() == null
                || event.eventType() == null
                || event.payload() == null) {
            throw new IllegalArgumentException(
                    "BusinessEvent and its required fields cannot be null");
        }

        String key = event.aggregateId();

        return kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, exception) -> {

                    if (exception != null) {
                        log.error(
                                "Failed to publish event: eventId={}, eventType={}",
                                event.eventId(),
                                event.eventType(),
                                exception
                        );
                        return;
                    }

                    var metadata = result.getRecordMetadata();

                    log.info(
                            "Business event published: eventId={}, eventType={}, topic={}, partition={}, offset={}",
                            event.eventId(),
                            event.eventType(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset()
                    );
                });
    }
}
