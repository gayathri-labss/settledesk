package com.settledesk.repository;

import com.settledesk.model.ProcessedEvent;
import com.settledesk.model.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_event (consumer_name, event_id, processed_at)
            SELECT :consumerName, :eventId, SYSUTCDATETIME()
            WHERE NOT EXISTS (
                SELECT 1
                FROM processed_event WITH (UPDLOCK, HOLDLOCK)
                WHERE consumer_name = :consumerName
                AND event_id = :eventId
            )
            """, nativeQuery = true)
    int insertIfNotExists(
            @Param("consumerName") String consumerName,
            @Param("eventId") java.util.UUID eventId
    );
}