package com.settledesk.messaging;

import com.settledesk.event.TradeCreatedEvent;
import com.settledesk.model.OutboxEvent;
import com.settledesk.model.OutboxStatus;
import com.settledesk.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final TradeEventProducer tradeEventProducer;
    private final JsonMapper jsonMapper;

    private final Set<UUID> eventsInProgress =
            ConcurrentHashMap.newKeySet();

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            TradeEventProducer tradeEventProducer,
            JsonMapper jsonMapper) {

        this.outboxEventRepository = outboxEventRepository;
        this.tradeEventProducer = tradeEventProducer;
        this.jsonMapper = jsonMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING
                );

        for (OutboxEvent outboxEvent : pendingEvents) {

            UUID eventId = outboxEvent.getEventId();

            if (!eventsInProgress.add(eventId)) {
                continue;
            }

            try {
                TradeCreatedEvent event = jsonMapper.readValue(
                        outboxEvent.getPayload(),
                        TradeCreatedEvent.class
                );

                tradeEventProducer.publishTradeCreated(event)
                        .whenComplete((result, exception) -> {

                            try {
                                if (exception != null) {
                                    log.error(
                                            "Failed to publish event: {}",
                                            eventId,
                                            exception
                                    );
                                    return;
                                }

                                outboxEvent.setStatus(
                                        OutboxStatus.PUBLISHED
                                );

                                outboxEvent.setPublishedAt(Instant.now());

                                outboxEventRepository.save(outboxEvent);

                                log.info(
                                        "Outbox event marked as PUBLISHED: {}",
                                        eventId
                                );

                            } catch (Exception e) {
                                log.error(
                                        "Failed to update Outbox event: {}",
                                        eventId,
                                        e
                                );
                            } finally {
                                eventsInProgress.remove(eventId);
                            }
                        });

            } catch (Exception e) {
                eventsInProgress.remove(eventId);

                log.error(
                        "Failed to read Outbox event: {}",
                        eventId,
                        e
                );
            }
        }
    }
}