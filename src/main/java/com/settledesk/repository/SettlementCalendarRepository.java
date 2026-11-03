package com.settledesk.repository;

import com.settledesk.model.SettlementCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementCalendarRepository
        extends JpaRepository<SettlementCalendar, Long> {
}
