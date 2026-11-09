
package com.settledesk.integration;

import com.settledesk.dto.ConfirmSettlementRequest;
import com.settledesk.dto.CreateSettlementRequest;
import com.settledesk.dto.CreateTradeConfirmationRequest;
import com.settledesk.dto.CreateTradeRequest;
import com.settledesk.dto.MatchingRunRequest;
import com.settledesk.dto.UpdateTradeStatusRequest;

import com.settledesk.event.BusinessEvent;
import com.settledesk.messaging.BusinessEventProducer;

import com.settledesk.model.MatchingOutcome;
import com.settledesk.model.ProcessedEvent;
import com.settledesk.model.ProcessedEventId;
import com.settledesk.model.SettlementOutcome;
import com.settledesk.model.SettlementStatus;
import com.settledesk.model.Trade;
import com.settledesk.model.TradeStatus;

import com.settledesk.repository.ProcessedEventRepository;

import com.settledesk.service.MatchingService;
import com.settledesk.service.SettlementService;
import com.settledesk.service.TradeConfirmationService;
import com.settledesk.service.TradeService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.settledesk.dto.ExternalSettlementInput;
import com.settledesk.dto.StartReconciliationRequest;
import com.settledesk.model.ReconciliationResultStatus;
import com.settledesk.model.ReconciliationRunStatus;
import com.settledesk.service.ReconciliationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
@Testcontainers
class TradeEventConsumerIntegrationTest {

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka-native:3.8.0")
            );

    @Container
    static final MSSQLServerContainer<?> sqlServer =
            new MSSQLServerContainer<>(
                    "mcr.microsoft.com/mssql/server:2022-latest"
            )
                    .acceptLicense()
                    .withPassword("TestPassword123!");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private BusinessEventProducer eventProducer;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeConfirmationService tradeConfirmationService;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private ReconciliationService reconciliationService;


    // -------------------------------------------------------
    // TEST 1: Kafka TradeCreated event consumption
    // -------------------------------------------------------

    @Test
    void shouldConsumeTradeCreatedEventAndPersistProcessedEvent()
            throws Exception {

        listenerRegistry.getListenerContainers().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, 1)
        );

        UUID eventId = UUID.randomUUID();

        var payload = new tools.jackson.databind.ObjectMapper()
                .readTree("""
                    {
                      "tradeId": 1001,
                      "instrument": "NIFTY",
                      "quantity": 10,
                      "price": 25000,
                      "buyer": "BUYER_A",
                      "seller": "SELLER_B",
                      "status": "CREATED"
                    }
                    """);

        BusinessEvent event = new BusinessEvent(
                eventId,
                "TradeCreated",
                "Trade",
                "1001",
                Instant.now(),
                1,
                payload
        );

        eventProducer.publishBusinessEvent(event)
                .get(10, SECONDS);

        ProcessedEventId id =
                new ProcessedEventId("trade-consumer-group", eventId);

        await()
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    ProcessedEvent processed =
                            processedEventRepository.findById(id).orElse(null);

                    assertThat(processed).isNotNull();
                    assertThat(processed.getProcessedAt()).isNotNull();
                });
    }


    // -------------------------------------------------------
    // TEST 2: Duplicate Kafka event handling
    // -------------------------------------------------------

    @Test
    void shouldIgnoreDuplicateTradeEvent() throws Exception {

        UUID eventId = UUID.randomUUID();

        var payload = new tools.jackson.databind.ObjectMapper()
                .readTree("""
                    {
                      "tradeId": 1002,
                      "instrument": "NIFTY",
                      "quantity": 5,
                      "price": 25000,
                      "buyer": "BUYER_A",
                      "seller": "SELLER_B",
                      "status": "CREATED"
                    }
                    """);

        BusinessEvent event = new BusinessEvent(
                eventId,
                "TradeCreated",
                "Trade",
                "1002",
                Instant.now(),
                1,
                payload
        );

        eventProducer.publishBusinessEvent(event).get(10, SECONDS);
        eventProducer.publishBusinessEvent(event).get(10, SECONDS);

        ProcessedEventId id =
                new ProcessedEventId("trade-consumer-group", eventId);

        await()
                .atMost(15, SECONDS)
                .untilAsserted(() ->
                        assertThat(processedEventRepository.findById(id))
                                .isPresent()
                );

        long matchingRecords = processedEventRepository.findAll()
                .stream()
                .filter(record -> record.getId().equals(id))
                .count();

        assertThat(matchingRecords).isEqualTo(1);
    }


    // -------------------------------------------------------
    // TEST 3: Complete trade-to-settlement lifecycle
    // -------------------------------------------------------

    @Test
    void shouldCompleteEndToEndTradeLifecycle() {

        // STEP 1: Create trade

        CreateTradeRequest tradeRequest = new CreateTradeRequest();
        tradeRequest.setInstrument("NIFTY");
        tradeRequest.setQuantity(new BigDecimal("10"));
        tradeRequest.setPrice(new BigDecimal("25000"));
        tradeRequest.setBuyer("BUYER_E2E");
        tradeRequest.setSeller("SELLER_E2E");

        Trade trade = tradeService.createTrade(tradeRequest);

        assertThat(trade.getTradeId()).isNotNull();
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.NEW);


        // STEP 2: Create counterparty confirmation

        CreateTradeConfirmationRequest confirmationRequest =
                new CreateTradeConfirmationRequest();

        confirmationRequest.setCounterpartyReference("E2E-REF-001");
        confirmationRequest.setInstrument("NIFTY");
        confirmationRequest.setQuantity(new BigDecimal("10"));
        confirmationRequest.setPrice(new BigDecimal("25000"));
        confirmationRequest.setBuyer("BUYER_E2E");
        confirmationRequest.setSeller("SELLER_E2E");

        var confirmation = tradeConfirmationService.createConfirmation(
                trade.getTradeId(),
                confirmationRequest
        );

        assertThat(confirmation.getConfirmationId()).isNotNull();


        // STEP 3: Execute matching

        MatchingRunRequest matchingRequest = new MatchingRunRequest();
        matchingRequest.setTradeIds(List.of(trade.getTradeId()));

        var matchingRun =
                matchingService.startMatchingRun(matchingRequest);

        assertThat(matchingRun.getMatchedCount()).isEqualTo(1);
        assertThat(matchingRun.getUnmatchedCount()).isZero();

        assertThat(
                matchingService.getMatchingResultsByRunId(
                        matchingRun.getRunId()
                )
        ).anySatisfy(result ->
                assertThat(result.getOutcome())
                        .isEqualTo(MatchingOutcome.MATCHED)
        );


        // STEP 4: Confirm matched trade

        UpdateTradeStatusRequest statusRequest =
                new UpdateTradeStatusRequest();

        statusRequest.setStatus(TradeStatus.CONFIRMED);

        tradeService.updateTradeStatus(
                trade.getTradeId(),
                statusRequest
        );

        assertThat(
                tradeService.getTradeByTradeId(trade.getTradeId()).getStatus()
        ).isEqualTo(TradeStatus.CONFIRMED);


        // STEP 5: Create settlement instruction

        CreateSettlementRequest settlementRequest =
                new CreateSettlementRequest();

        settlementRequest.setTradeId(trade.getTradeId());
        settlementRequest.setSettlementDate(LocalDate.now().plusDays(1));

        var settlement =
                settlementService.createSettlementInstruction(
                        settlementRequest
                );

        assertThat(settlement.getSettlementId()).isNotNull();
        assertThat(settlement.getStatus())
                .isEqualTo(SettlementStatus.CREATED);


        // STEP 6: Submit settlement

        var submittedSettlement =
                settlementService.submitSettlement(
                        settlement.getSettlementId()
                );

        assertThat(submittedSettlement.getStatus())
                .isEqualTo(SettlementStatus.SUBMITTED);


        // STEP 7: Confirm successful settlement

        ConfirmSettlementRequest settleRequest =
                new ConfirmSettlementRequest();

        settleRequest.setOutcome(SettlementOutcome.SETTLED);

        var completedSettlement =
                settlementService.confirmSettlement(
                        settlement.getSettlementId(),
                        settleRequest
                );

        assertThat(completedSettlement.getStatus())
                .isEqualTo(SettlementStatus.SETTLED);


        // STEP 8: Verify final trade status

        Trade finalTrade =
                tradeService.getTradeByTradeId(trade.getTradeId());

        assertThat(finalTrade.getStatus())
                .isEqualTo(TradeStatus.SETTLED);


        // STEP 9: Start reconciliation using the actual settled instruction

        ExternalSettlementInput externalRecord = new ExternalSettlementInput();

        externalRecord.setExternalReference("E2E-EXT-" + UUID.randomUUID());
        externalRecord.setSettlementReference(
                completedSettlement.getSettlementReference()
        );
        externalRecord.setInstrument(finalTrade.getInstrument());
        externalRecord.setQuantity(finalTrade.getQuantity());
        externalRecord.setPrice(finalTrade.getPrice());
        externalRecord.setSettlementDate(
                completedSettlement.getSettlementDate()
        );
        externalRecord.setStatus(completedSettlement.getStatus().name());

        StartReconciliationRequest reconciliationRequest =
                new StartReconciliationRequest();

        reconciliationRequest.setSource("E2E_TEST");
        reconciliationRequest.setSettlementDate(
                completedSettlement.getSettlementDate()
        );
        reconciliationRequest.setExternalRecords(List.of(externalRecord));

// STEP 10: Execute reconciliation

        var reconciliationRun =
                reconciliationService.startReconciliation(reconciliationRequest);

        assertThat(reconciliationRun.getRunId()).isNotNull();
        assertThat(reconciliationRun.getStatus())
                .isEqualTo(ReconciliationRunStatus.COMPLETED);

        assertThat(reconciliationRun.getTotalInternalRecords()).isEqualTo(1);
        assertThat(reconciliationRun.getTotalExternalRecords()).isEqualTo(1);
        assertThat(reconciliationRun.getMatchedCount()).isEqualTo(1);
        assertThat(reconciliationRun.getMismatchedCount()).isZero();
        assertThat(reconciliationRun.getMissingInternalCount()).isZero();
        assertThat(reconciliationRun.getMissingExternalCount()).isZero();

// STEP 11: Verify persisted reconciliation run

        var persistedRun =
                reconciliationService.getReconciliationRun(
                        reconciliationRun.getRunId()
                );

        assertThat(persistedRun.getStatus())
                .isEqualTo(ReconciliationRunStatus.COMPLETED);

// STEP 12: Verify persisted reconciliation result

        var results =
                reconciliationService.getReconciliationResults(
                        reconciliationRun.getRunId()
                );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus())
                .isEqualTo(ReconciliationResultStatus.MATCHED);
    }


}
