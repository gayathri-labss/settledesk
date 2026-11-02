package com.settledesk.repository;

import com.settledesk.model.MatchingRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingRunRepository
        extends JpaRepository<MatchingRun, Long> {
}