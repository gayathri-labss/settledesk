
package com.settledesk.integration;

import com.settledesk.dto.CreateTradeRequest;
import com.settledesk.model.Trade;
import com.settledesk.model.TradeAuditAction;
import com.settledesk.service.TradeAuditService;
import com.settledesk.service.TradeService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class SqlServerIntegrationTest {

    @Container
    static MSSQLServerContainer<?> sqlServer =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TradeService tradeService;

    @MockitoBean
    private TradeAuditService tradeAuditService;

    @Test
    void shouldConnectToSqlServerAndExecuteQuery() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1", Integer.class
        );

        assertEquals(1, result);
    }

    @Test
    void shouldRollbackTradeOutboxAndLifecycleWhenAuditCreationFails() {

        int tradesBefore = countRows("trade");
        int outboxBefore = countRows("outbox_event");
        int lifecycleBefore = countRows("trade_lifecycle");
        int auditBefore = countRows("trade_audit");

        doThrow(new IllegalStateException("Simulated audit failure"))
                .when(tradeAuditService)
                .recordAudit(
                        any(Trade.class),
                        eq(TradeAuditAction.CREATE),
                        eq("Trade created")
                );

        CreateTradeRequest request = new CreateTradeRequest();
        request.setInstrument("ROLLBACK_TEST");
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("100"));
        request.setBuyer("BUYER_TEST");
        request.setSeller("SELLER_TEST");

        assertThrows(
                IllegalStateException.class,
                () -> tradeService.createTrade(request)
        );

        assertEquals(tradesBefore, countRows("trade"),
                "Trade should be rolled back");

        assertEquals(outboxBefore, countRows("outbox_event"),
                "Outbox event should be rolled back");

        assertEquals(lifecycleBefore, countRows("trade_lifecycle"),
                "Lifecycle record should be rolled back");

        assertEquals(auditBefore, countRows("trade_audit"),
                "Audit record should not be persisted");
    }

    private int countRows(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Integer.class
        );
    }
}
