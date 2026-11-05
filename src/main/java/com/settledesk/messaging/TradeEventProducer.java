
package com.settledesk.messaging;

import com.settledesk.event.TradeCreatedEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class TradeEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(TradeEventProducer.class);

    private static final String TOPIC = "trade-events";

    private final KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate;
    private final Validator validator;

    public TradeEventProducer(
            KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate,
            Validator validator) {

        this.kafkaTemplate = kafkaTemplate;
        this.validator = validator;
    }

    public CompletableFuture<SendResult<String, TradeCreatedEvent>>
    publishTradeCreated(TradeCreatedEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "TradeCreatedEvent cannot be null");
        }

        Set<ConstraintViolation<TradeCreatedEvent>> violations =
                validator.validate(event);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        String key = event.tradeId().toString();

        return kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, exception) -> {

                    if (exception != null) {
                        log.error(
                                "Failed to publish TradeCreatedEvent for tradeId={}",
                                event.tradeId(),
                                exception
                        );
                        return;
                    }

                    var metadata = result.getRecordMetadata();

                    log.info(
                            "Trade event published: tradeId={}, topic={}, partition={}, offset={}",
                            event.tradeId(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset()
                    );
                });
    }
}
