
package com.settledesk.service;

import com.settledesk.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedEventServiceTest {

    @Mock
    private ProcessedEventRepository repository;

    private ProcessedEventService service;

    @BeforeEach
    void setUp() {
        service = new ProcessedEventService(repository);
    }

    @Test
    void markProcessedIfNew_shouldReturnTrueForNewEvent() {
        UUID eventId = UUID.randomUUID();

        when(repository.insertIfNotExists("trade-consumer", eventId))
                .thenReturn(1);

        assertTrue(service.markProcessedIfNew("trade-consumer", eventId));

        verify(repository).insertIfNotExists("trade-consumer", eventId);
    }

    @Test
    void markProcessedIfNew_shouldReturnFalseForDuplicateEvent() {
        UUID eventId = UUID.randomUUID();

        when(repository.insertIfNotExists("trade-consumer", eventId))
                .thenReturn(0);

        assertFalse(service.markProcessedIfNew("trade-consumer", eventId));

        verify(repository).insertIfNotExists("trade-consumer", eventId);
    }

    @Test
    void markProcessedIfNew_shouldRejectNullConsumerName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.markProcessedIfNew(null, UUID.randomUUID())
        );

        verifyNoInteractions(repository);
    }

    @Test
    void markProcessedIfNew_shouldRejectBlankConsumerName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.markProcessedIfNew("  ", UUID.randomUUID())
        );

        verifyNoInteractions(repository);
    }

    @Test
    void markProcessedIfNew_shouldRejectNullEventId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.markProcessedIfNew("trade-consumer", null)
        );

        verifyNoInteractions(repository);
    }
}
