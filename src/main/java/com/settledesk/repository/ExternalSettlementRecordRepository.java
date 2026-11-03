package com.settledesk.repository;

import com.settledesk.model.ExternalSettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalSettlementRecordRepository
        extends JpaRepository<ExternalSettlementRecord, Long> {

    boolean existsBySourceAndExternalReference(
            String source,
            String externalReference
    );
}