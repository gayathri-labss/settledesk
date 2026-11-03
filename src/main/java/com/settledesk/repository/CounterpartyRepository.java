package com.settledesk.repository;

import com.settledesk.model.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CounterpartyRepository
        extends JpaRepository<Counterparty, Long>,
        JpaSpecificationExecutor<Counterparty> {

}