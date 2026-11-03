package com.settledesk.repository;

public interface ReconciliationResultRepository
        extends JpaRepository<ReconciliationResult, Long> {

    List<ReconciliationResult> findByRun_RunId(Long runId);
}
