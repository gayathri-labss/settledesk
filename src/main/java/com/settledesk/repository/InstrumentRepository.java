package com.settledesk.repository;

import com.settledesk.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstrumentRepository
        extends JpaRepository<Instrument, Long>,
        JpaSpecificationExecutor<Instrument> {

}