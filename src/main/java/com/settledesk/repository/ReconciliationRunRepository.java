package com.settledesk.repository;

import com.settledesk.model.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRunRepository
        extends JpaRepository<ReconciliationRun, Long> {
}
