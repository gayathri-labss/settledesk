
package com.settledesk.service;

import com.settledesk.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean markProcessedIfNew(String consumerName, UUID eventId) {

        if (consumerName == null || consumerName.isBlank()) {
            throw new IllegalArgumentException("Consumer name cannot be blank");
        }

        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }

        int inserted = processedEventRepository.insertIfNotExists(
                consumerName,
                eventId
        );

        return inserted == 1;
    }
}
