package com.settledesk.repository;

import com.settledesk.model.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingJobRepository
        extends JpaRepository<ProcessingJob, Long> {
}
