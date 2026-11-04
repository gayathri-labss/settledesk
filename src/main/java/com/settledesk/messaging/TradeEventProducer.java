package com.settledesk.messaging;

import com.settledesk.event.TradeCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TradeEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(TradeEventProducer.class);

    private static final String TOPIC = "trade-events";

    private final KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate;

    public TradeEventProducer(
            KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, TradeCreatedEvent>>
    publishTradeCreated(TradeCreatedEvent event) {

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