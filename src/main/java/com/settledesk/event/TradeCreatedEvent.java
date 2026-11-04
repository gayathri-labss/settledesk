package com.settledesk.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeCreatedEvent(
        UUID eventId,
        Long tradeId,
        String instrument,
        BigDecimal quantity,
        BigDecimal price,
        String buyer,
        String seller,
        String status,
        Instant occurredAt
) {
}