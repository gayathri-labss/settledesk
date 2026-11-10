
package com.settledesk.event;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record BusinessEvent(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        int version,
        JsonNode payload
) {
}
