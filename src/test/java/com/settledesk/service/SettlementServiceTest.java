
package com.settledesk.service;

import com.settledesk.dto.ConfirmSettlementRequest;
import com.settledesk.dto.CreateSettlementRequest;
import com.settledesk.dto.UpdateTradeStatusRequest;
import com.settledesk.event.SettlementCompletedEvent;
import com.settledesk.event.SettlementFailedEvent;
import com.settledesk.event.SettlementInstructionCreatedEvent;
import com.settledesk.model.*;
import com.settledesk.repository.SettlementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private TradeService tradeService;

    @Mock
    private SettlementLifecycleService settlementLifecycleService;

    @Mock
    private SettlementAuditService settlementAuditService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private SettlementService settlementService;

    private Trade trade;

    @BeforeEach
    void setUp() {
        trade = new Trade();
        trade.setTradeId(70001L);
        trade.setStatus(TradeStatus.CONFIRMED);
    }

    @Test
    void createSettlementInstruction_shouldSaveAndPublishEvent() {
        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setTradeId(70001L);
        request.setSettlementDate(LocalDate.of(2026, 11, 10));

        when(tradeService.getTradeByTradeId(70001L)).thenReturn(trade);
        when(settlementRepository.existsByTrade_TradeId(70001L))
                .thenReturn(false);
        when(settlementRepository.save(any(SettlementInstruction.class)))
                .thenAnswer(invocation -> {
                    SettlementInstruction instruction = invocation.getArgument(0);
                    instruction.setSettlementId(10001L);
                    return instruction;
                });

        SettlementInstruction result =
                settlementService.createSettlementInstruction(request);

        assertThat(result.getSettlementId()).isEqualTo(10001L);
        assertThat(result.getStatus()).isEqualTo(SettlementStatus.CREATED);
        assertThat(result.getSettlementReference()).startsWith("SET-");
        assertThat(result.getTrade()).isSameAs(trade);

        verify(outboxService).saveSettlementInstructionCreatedEvent(
                any(SettlementInstructionCreatedEvent.class));
        verify(settlementLifecycleService).recordStatusChange(
                result, null, SettlementStatus.CREATED);
        verify(settlementAuditService).recordAudit(
                result,
                SettlementAuditAction.CREATE,
                "Settlement instruction created");
    }

    @Test
    void createSettlementInstruction_shouldRejectUnconfirmedTrade() {
        trade.setStatus(TradeStatus.NEW);

        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setTradeId(70001L);
        request.setSettlementDate(LocalDate.of(2026, 11, 10));

        when(tradeService.getTradeByTradeId(70001L)).thenReturn(trade);

        assertThatThrownBy(() ->
                settlementService.createSettlementInstruction(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(settlementRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void createSettlementInstruction_shouldRejectDuplicateInstruction() {
        CreateSettlementRequest request = new CreateSettlementRequest();
        request.setTradeId(70001L);
        request.setSettlementDate(LocalDate.of(2026, 11, 10));

        when(tradeService.getTradeByTradeId(70001L)).thenReturn(trade);
        when(settlementRepository.existsByTrade_TradeId(70001L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                settlementService.createSettlementInstruction(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(settlementRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void submitSettlement_shouldSubmitCreatedInstruction() {
        SettlementInstruction instruction = createInstruction(
                10001L, SettlementStatus.CREATED);

        when(settlementRepository.findById(10001L))
                .thenReturn(Optional.of(instruction));
        when(settlementRepository.save(any(SettlementInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettlementInstruction result =
                settlementService.submitSettlement(10001L);

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();

        verify(tradeService).updateTradeStatus(
                eq(70001L), any(UpdateTradeStatusRequest.class));
        verify(settlementLifecycleService).recordStatusChange(
                instruction, SettlementStatus.CREATED, SettlementStatus.SUBMITTED);
        verify(settlementAuditService).recordAudit(
                eq(instruction),
                eq(SettlementAuditAction.SUBMIT),
                contains("CREATED"));
    }

    @Test
    void confirmSettlement_shouldSettleAndPublishCompletedEvent() {
        SettlementInstruction instruction = createInstruction(
                10001L, SettlementStatus.SUBMITTED);

        ConfirmSettlementRequest request = new ConfirmSettlementRequest();
        request.setOutcome(SettlementOutcome.SETTLED);

        when(settlementRepository.findById(10001L))
                .thenReturn(Optional.of(instruction));
        when(settlementRepository.save(any(SettlementInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettlementInstruction result =
                settlementService.confirmSettlement(10001L, request);

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.SETTLED);
        assertThat(result.getConfirmedAt()).isNotNull();
        assertThat(result.getFailureReason()).isNull();

        verify(tradeService).updateTradeStatus(
                eq(70001L), any(UpdateTradeStatusRequest.class));
        verify(outboxService).saveSettlementCompletedEvent(
                any(SettlementCompletedEvent.class));
        verify(outboxService, never()).saveSettlementFailedEvent(any());
    }

    @Test
    void confirmSettlement_shouldFailAndPublishFailedEvent() {
        SettlementInstruction instruction = createInstruction(
                10001L, SettlementStatus.SUBMITTED);

        ConfirmSettlementRequest request = new ConfirmSettlementRequest();
        request.setOutcome(SettlementOutcome.FAILED);
        request.setFailureReason("Insufficient funds");

        when(settlementRepository.findById(10001L))
                .thenReturn(Optional.of(instruction));
        when(settlementRepository.save(any(SettlementInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettlementInstruction result =
                settlementService.confirmSettlement(10001L, request);

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Insufficient funds");
        assertThat(result.getConfirmedAt()).isNotNull();

        verify(outboxService).saveSettlementFailedEvent(
                any(SettlementFailedEvent.class));
        verify(outboxService, never()).saveSettlementCompletedEvent(any());
        verify(tradeService, never()).updateTradeStatus(anyLong(), any());
    }

    @Test
    void confirmSettlement_shouldRejectWithoutPublishingFailureEvent() {
        SettlementInstruction instruction = createInstruction(
                10001L, SettlementStatus.SUBMITTED);

        ConfirmSettlementRequest request = new ConfirmSettlementRequest();
        request.setOutcome(SettlementOutcome.REJECTED);
        request.setFailureReason("Counterparty rejected settlement");

        when(settlementRepository.findById(10001L))
                .thenReturn(Optional.of(instruction));
        when(settlementRepository.save(any(SettlementInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettlementInstruction result =
                settlementService.confirmSettlement(10001L, request);

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.REJECTED);
        assertThat(result.getFailureReason())
                .isEqualTo("Counterparty rejected settlement");

        verify(outboxService, never()).saveSettlementFailedEvent(any());
        verify(outboxService, never()).saveSettlementCompletedEvent(any());
        verify(tradeService, never()).updateTradeStatus(anyLong(), any());
    }

    private SettlementInstruction createInstruction(
            Long settlementId,
            SettlementStatus status) {

        return SettlementInstruction.builder()
                .settlementId(settlementId)
                .trade(trade)
                .settlementReference("SET-TEST-" + settlementId)
                .settlementDate(LocalDate.of(2026, 11, 10))
                .status(status)
                .build();
    }
}
