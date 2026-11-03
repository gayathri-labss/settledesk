package com.settledesk.repository;

import com.settledesk.model.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationResultRepository
        extends JpaRepository<ReconciliationResult, Long> {

    List<ReconciliationResult> findByRun_RunId(Long runId);
}
