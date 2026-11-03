package com.settledesk.repository;

import com.settledesk.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReconciliationBreakRepository extends JpaRepository<ReconciliationBreak, Long> {

    @Query("""
        SELECT b FROM ReconciliationBreak b
        WHERE (:status IS NULL OR b.status = :status)
        AND (:category IS NULL OR b.category = :category)
        AND (:runId IS NULL OR b.result.run.runId = :runId)
    """)
    Page<ReconciliationBreak> searchBreaks(
            @Param("status") BreakStatus status,
            @Param("category") BreakCategory category,
            @Param("runId") Long runId,
            Pageable pageable
    );
}