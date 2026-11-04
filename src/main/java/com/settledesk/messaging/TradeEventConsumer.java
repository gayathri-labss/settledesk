package com.settledesk.messaging;

import com.settledesk.event.TradeCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TradeEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TradeEventConsumer.class);

    @KafkaListener(topics = "trade-events")
    public void consumeTradeCreated(TradeCreatedEvent event) {

        log.info(
                "Received TradeCreatedEvent: eventId={}, tradeId={}, instrument={}, quantity={}, price={}, status={}",
                event.eventId(),
                event.tradeId(),
                event.instrument(),
                event.quantity(),
                event.price(),
                event.status()
        );
    }
}