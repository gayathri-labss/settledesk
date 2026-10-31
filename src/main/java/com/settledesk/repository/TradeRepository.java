package com.settledesk.repository;

import com.settledesk.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

//JpaRepository<Trade, Long> → Standard database operations, pagination, and sorting.
//JpaSpecificationExecutor<Trade> → Execute queries built dynamically using Specifications.
public interface TradeRepository extends JpaRepository<Trade, Long>, JpaSpecificationExecutor<Trade> {

}