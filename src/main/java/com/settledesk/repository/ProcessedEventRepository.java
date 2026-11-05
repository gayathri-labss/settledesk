package com.settledesk.repository;

import com.settledesk.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_event (event_id, processed_at)
            SELECT :eventId, SYSUTCDATETIME()
            WHERE NOT EXISTS (
                SELECT 1
                FROM processed_event WITH (UPDLOCK, HOLDLOCK)
                WHERE event_id = :eventId
            )
            """, nativeQuery = true)
    int insertIfNotExists(@Param("eventId") UUID eventId);
}