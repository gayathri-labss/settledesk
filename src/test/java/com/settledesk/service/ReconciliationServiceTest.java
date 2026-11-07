
package com.settledesk.service;

import com.settledesk.dto.ExternalSettlementInput;
import com.settledesk.dto.StartReconciliationRequest;
import com.settledesk.model.*;
import com.settledesk.repository.*;
import com.settledesk.event.ReconciliationCompletedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private ReconciliationRunRepository runRepository;

    @Mock
    private ReconciliationResultRepository resultRepository;

    @Mock
    private ReconciliationBreakRepository breakRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private ReconciliationService reconciliationService;

    private final LocalDate settlementDate =
            LocalDate.of(2026, 11, 10);

    private SettlementInstruction internalSettlement;

    @BeforeEach
    void setUp() {
        Trade trade = Trade.builder()
                .tradeId(70001L)
                .instrument("AAPL")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("210.50"))
                .status(TradeStatus.SETTLED)
                .build();

        internalSettlement = SettlementInstruction.builder()
                .settlementId(10001L)
                .trade(trade)
                .settlementReference("SET-10001")
                .settlementDate(settlementDate)
                .status(SettlementStatus.SETTLED)
                .build();
    }

    @Test
    void startReconciliation_shouldCompleteWhenAllRecordsMatch() {
        StartReconciliationRequest request =
                createRequest(List.of(createExternalRecord(
                        "EXT-10001",
                        "SET-10001",
                        "AAPL",
                        "100",
                        "210.50",
                        "SETTLED"
                )));

        when(settlementRepository.findBySettlementDate(settlementDate))
                .thenReturn(List.of(internalSettlement));

        mockRunSave();

        when(breakRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationRun result =
                reconciliationService.startReconciliation(request);

        assertThat(result.getStatus())
                .isEqualTo(ReconciliationRunStatus.COMPLETED);
        assertThat(result.getTotalInternalRecords()).isEqualTo(1);
        assertThat(result.getTotalExternalRecords()).isEqualTo(1);
        assertThat(result.getMatchedCount()).isEqualTo(1);
        assertThat(result.getMismatchedCount()).isZero();
        assertThat(result.getMissingInternalCount()).isZero();
        assertThat(result.getMissingExternalCount()).isZero();
        assertThat(result.getCompletedAt()).isNotNull();

        verify(resultRepository).saveAll(argThat(results -> {
            var iterator = results.iterator();

            if (!iterator.hasNext()) {
                return false;
            }

            ReconciliationResult reconciliationResult = iterator.next();

            return reconciliationResult.getStatus()
                    == ReconciliationResultStatus.MATCHED
                    && !iterator.hasNext();
        }));

        verify(breakRepository).saveAll(argThat(breaks ->
                !breaks.iterator().hasNext()));

        verify(outboxService).saveReconciliationCompletedEvent(
                any(ReconciliationCompletedEvent.class));

        verify(outboxService, never())
                .saveReconciliationBreakDetectedEvent(any());
    }

    @Test
    void startReconciliation_shouldDetectMissingInternalRecord() {
        StartReconciliationRequest request =
                createRequest(List.of(createExternalRecord(
                        "EXT-20001",
                        "UNKNOWN-SETTLEMENT",
                        "AAPL",
                        "100",
                        "210.50",
                        "SETTLED"
                )));

        when(settlementRepository.findBySettlementDate(settlementDate))
                .thenReturn(List.of());

        mockRunSave();
        mockBreakSave();

        ReconciliationRun result =
                reconciliationService.startReconciliation(request);

        assertThat(result.getStatus())
                .isEqualTo(ReconciliationRunStatus.COMPLETED);
        assertThat(result.getMissingInternalCount()).isEqualTo(1);
        assertThat(result.getMatchedCount()).isZero();

        verify(outboxService).saveReconciliationBreakDetectedEvent(
                argThat(event ->
                        event.category() == BreakCategory.MISSING_INTERNAL));

        verify(outboxService).saveReconciliationCompletedEvent(
                any(ReconciliationCompletedEvent.class));
    }

    @Test
    void startReconciliation_shouldDetectMissingExternalRecord() {
        StartReconciliationRequest request =
                createRequest(List.of());

        when(settlementRepository.findBySettlementDate(settlementDate))
                .thenReturn(List.of(internalSettlement));

        mockRunSave();
        mockBreakSave();

        ReconciliationRun result =
                reconciliationService.startReconciliation(request);

        assertThat(result.getStatus())
                .isEqualTo(ReconciliationRunStatus.COMPLETED);
        assertThat(result.getMissingExternalCount()).isEqualTo(1);
        assertThat(result.getMatchedCount()).isZero();

        verify(outboxService).saveReconciliationBreakDetectedEvent(
                argThat(event ->
                        event.category() == BreakCategory.MISSING_EXTERNAL));

        verify(outboxService).saveReconciliationCompletedEvent(
                any(ReconciliationCompletedEvent.class));
    }

    @Test
    void startReconciliation_shouldDetectMismatchedSettlement() {
        StartReconciliationRequest request =
                createRequest(List.of(createExternalRecord(
                        "EXT-30001",
                        "SET-10001",
                        "AAPL",
                        "100",
                        "215.00",
                        "SETTLED"
                )));

        when(settlementRepository.findBySettlementDate(settlementDate))
                .thenReturn(List.of(internalSettlement));

        mockRunSave();
        mockBreakSave();

        ReconciliationRun result =
                reconciliationService.startReconciliation(request);

        assertThat(result.getStatus())
                .isEqualTo(ReconciliationRunStatus.COMPLETED);
        assertThat(result.getMismatchedCount()).isEqualTo(1);
        assertThat(result.getMatchedCount()).isZero();

        verify(resultRepository).saveAll(argThat(results -> {
            var iterator = results.iterator();

            if (!iterator.hasNext()) {
                return false;
            }

            ReconciliationResult reconciliationResult = iterator.next();

            return reconciliationResult.getStatus()
                    == ReconciliationResultStatus.MISMATCHED
                    && reconciliationResult.getMismatchDetails()
                    .contains("Price mismatch")
                    && !iterator.hasNext();
        }));

        verify(outboxService).saveReconciliationBreakDetectedEvent(
                argThat(event ->
                        event.category() == BreakCategory.MISMATCH));
    }

    @Test
    void startReconciliation_shouldRejectDuplicateExternalReference() {
        ExternalSettlementInput first = createExternalRecord(
                "EXT-DUPLICATE",
                "SET-10001",
                "AAPL",
                "100",
                "210.50",
                "SETTLED"
        );

        ExternalSettlementInput second = createExternalRecord(
                "EXT-DUPLICATE",
                "SET-10002",
                "MSFT",
                "200",
                "420.75",
                "SETTLED"
        );

        StartReconciliationRequest request =
                createRequest(List.of(first, second));

        when(settlementRepository.findBySettlementDate(settlementDate))
                .thenReturn(List.of());

        mockRunSave();

        assertThatThrownBy(() ->
                reconciliationService.startReconciliation(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(resultRepository, never()).saveAll(anyList());
        verify(outboxService, never())
                .saveReconciliationCompletedEvent(any());
    }

    private StartReconciliationRequest createRequest(
            List<ExternalSettlementInput> records) {

        StartReconciliationRequest request =
                new StartReconciliationRequest();

        request.setSource("TEST_SOURCE");
        request.setSettlementDate(settlementDate);
        request.setExternalRecords(records);

        return request;
    }

    private ExternalSettlementInput createExternalRecord(
            String externalReference,
            String settlementReference,
            String instrument,
            String quantity,
            String price,
            String status) {

        ExternalSettlementInput input = new ExternalSettlementInput();

        input.setExternalReference(externalReference);
        input.setSettlementReference(settlementReference);
        input.setInstrument(instrument);
        input.setQuantity(new BigDecimal(quantity));
        input.setPrice(new BigDecimal(price));
        input.setSettlementDate(settlementDate);
        input.setStatus(status);

        return input;
    }

    private void mockRunSave() {
        when(runRepository.save(any(ReconciliationRun.class)))
                .thenAnswer(invocation -> {
                    ReconciliationRun run = invocation.getArgument(0);
                    run.setRunId(50001L);
                    return run;
                });
    }

    private void mockBreakSave() {
        when(breakRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<ReconciliationBreak> breaks =
                            invocation.getArgument(0);

                    long id = 60001L;

                    for (ReconciliationBreak reconciliationBreak : breaks) {
                        reconciliationBreak.setBreakId(id++);
                    }

                    return breaks;
                });
    }
}
