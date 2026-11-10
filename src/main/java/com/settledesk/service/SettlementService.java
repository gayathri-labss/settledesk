package com.settledesk.service;

import com.settledesk.dto.CreateSettlementRequest;
import com.settledesk.dto.UpdateTradeStatusRequest;
import com.settledesk.event.SettlementCompletedEvent;
import com.settledesk.event.SettlementFailedEvent;
import com.settledesk.event.SettlementInstructionCreatedEvent;
import com.settledesk.model.*;
import com.settledesk.repository.SettlementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.settledesk.dto.ConfirmSettlementRequest;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final TradeService tradeService;
    private final SettlementLifecycleService settlementLifecycleService;
    private final SettlementAuditService settlementAuditService;
    private final OutboxService outboxService;

    public SettlementService(
            SettlementRepository settlementRepository,
            TradeService tradeService,
            SettlementLifecycleService settlementLifecycleService,
            SettlementAuditService settlementAuditService,
            OutboxService outboxService) {

        this.settlementRepository = settlementRepository;
        this.tradeService = tradeService;
        this.settlementLifecycleService = settlementLifecycleService;
        this.settlementAuditService = settlementAuditService;
        this.outboxService = outboxService;
    }

    @Transactional
    public SettlementInstruction createSettlementInstruction(
            CreateSettlementRequest request) {

        Trade trade = tradeService.getTradeByTradeId(
                request.getTradeId()
        );

        if (trade.getStatus() != TradeStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Settlement can only be created for confirmed trades"
            );
        }

        if (settlementRepository.existsByTrade_TradeId(
                trade.getTradeId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Settlement instruction already exists for trade: "
                            + trade.getTradeId()
            );
        }

        SettlementInstruction instruction =
                SettlementInstruction.builder()
                        .trade(trade)
                        .settlementReference(
                                "SET-" + UUID.randomUUID()
                        )
                        .settlementDate(request.getSettlementDate())
                        .status(SettlementStatus.CREATED)
                        .createdAt(LocalDateTime.now())
                        .build();

        SettlementInstruction savedInstruction =
                settlementRepository.save(instruction);

        SettlementInstructionCreatedEvent event =
                new SettlementInstructionCreatedEvent(
                        savedInstruction.getSettlementId(),
                        savedInstruction.getTrade().getTradeId(),
                        savedInstruction.getSettlementReference(),
                        savedInstruction.getSettlementDate(),
                        savedInstruction.getStatus(),
                        savedInstruction.getCreatedAt()
                );

        outboxService.saveSettlementInstructionCreatedEvent(event);

        settlementLifecycleService.recordStatusChange(
                savedInstruction,
                null,
                SettlementStatus.CREATED
        );

        settlementAuditService.recordAudit(
                savedInstruction,
                SettlementAuditAction.CREATE,
                "Settlement instruction created"
        );

        return savedInstruction;    }

    @Transactional(readOnly = true)
    public List<SettlementInstruction> getAllSettlements() {
        return settlementRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SettlementInstruction getSettlementById(Long settlementId) {

        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Settlement not found with ID: " + settlementId
                ));
    }

    @Transactional
    public SettlementInstruction submitSettlement(Long settlementId) {

        SettlementInstruction instruction =
                getSettlementById(settlementId);

        SettlementStatus previousStatus = instruction.getStatus();

        if (instruction.getStatus() != SettlementStatus.CREATED
                && instruction.getStatus() != SettlementStatus.RETRY_PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only CREATED or RETRY_PENDING settlements can be submitted"
            );
        }

        if (instruction.getStatus() == SettlementStatus.CREATED) {

            UpdateTradeStatusRequest statusRequest =
                    new UpdateTradeStatusRequest();

            statusRequest.setStatus(TradeStatus.SETTLEMENT_PENDING);

            tradeService.updateTradeStatus(
                    instruction.getTrade().getTradeId(),
                    statusRequest
            );
        }

        instruction.setStatus(SettlementStatus.SUBMITTED);
        instruction.setSubmittedAt(LocalDateTime.now());

        SettlementInstruction savedInstruction = settlementRepository.save(instruction);

        settlementLifecycleService.recordStatusChange(
                savedInstruction,
                previousStatus,
                SettlementStatus.SUBMITTED
        );

        settlementAuditService.recordAudit(
                savedInstruction,
                SettlementAuditAction.SUBMIT,
                "Settlement instruction submitted from " + previousStatus
        );

        return savedInstruction;
    }

    @Transactional
    public SettlementInstruction confirmSettlement(
            Long settlementId,
            ConfirmSettlementRequest request) {

        SettlementInstruction instruction =
                getSettlementById(settlementId);

        SettlementStatus previousStatus = instruction.getStatus();

        if (instruction.getStatus() != SettlementStatus.SUBMITTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only SUBMITTED settlements can be confirmed"
            );
        }

        SettlementOutcome outcome = request.getOutcome();

        if (outcome == SettlementOutcome.SETTLED) {

            instruction.setStatus(SettlementStatus.SETTLED);
            instruction.setFailureReason(null);

            UpdateTradeStatusRequest statusRequest =
                    new UpdateTradeStatusRequest();

            statusRequest.setStatus(TradeStatus.SETTLED);

            tradeService.updateTradeStatus(
                    instruction.getTrade().getTradeId(),
                    statusRequest
            );

        } else {

            if (request.getFailureReason() == null
                    || request.getFailureReason().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Failure reason is required for unsuccessful settlement"
                );
            }

            instruction.setStatus(
                    outcome == SettlementOutcome.FAILED
                            ? SettlementStatus.FAILED
                            : SettlementStatus.REJECTED
            );

            instruction.setFailureReason(request.getFailureReason());
        }

        instruction.setConfirmedAt(LocalDateTime.now());

        SettlementInstruction savedInstruction =
                settlementRepository.save(instruction);
        if (outcome == SettlementOutcome.FAILED) {

            SettlementFailedEvent event =
                    new SettlementFailedEvent(
                            savedInstruction.getSettlementId(),
                            savedInstruction.getTrade().getTradeId(),
                            savedInstruction.getSettlementReference(),
                            savedInstruction.getSettlementDate(),
                            savedInstruction.getFailureReason(),
                            savedInstruction.getStatus(),
                            savedInstruction.getConfirmedAt()
                    );

            outboxService.saveSettlementFailedEvent(event);
        }

        if (outcome == SettlementOutcome.SETTLED) {

            SettlementCompletedEvent event =
                    new SettlementCompletedEvent(
                            savedInstruction.getSettlementId(),
                            savedInstruction.getTrade().getTradeId(),
                            savedInstruction.getSettlementReference(),
                            savedInstruction.getSettlementDate(),
                            savedInstruction.getStatus(),
                            savedInstruction.getConfirmedAt()
                    );

            outboxService.saveSettlementCompletedEvent(event);
        }

        settlementLifecycleService.recordStatusChange(
                savedInstruction,
                previousStatus,
                instruction.getStatus()
        );

        String details = "Settlement outcome recorded: " + request.getOutcome();

        if (request.getFailureReason() != null
                && !request.getFailureReason().isBlank()) {
            details += ". Reason: " + request.getFailureReason();
        }

        settlementAuditService.recordAudit(
                savedInstruction,
                SettlementAuditAction.CONFIRM,
                details
        );

        return savedInstruction;    }

    @Transactional
    public SettlementInstruction retrySettlement(Long settlementId) {

        SettlementInstruction instruction =
                getSettlementById(settlementId);

        SettlementStatus previousStatus = instruction.getStatus();

        if (instruction.getStatus() != SettlementStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only FAILED settlements can be retried"
            );
        }

        instruction.setStatus(SettlementStatus.RETRY_PENDING);

        SettlementInstruction savedInstruction =
                settlementRepository.save(instruction);

        settlementLifecycleService.recordStatusChange(
                savedInstruction,
                previousStatus,
                SettlementStatus.RETRY_PENDING
        );

        settlementAuditService.recordAudit(
                savedInstruction,
                SettlementAuditAction.RETRY,
                "Settlement retry requested"
        );

        return savedInstruction;
    }

    @Transactional(readOnly = true)
    public List<SettlementLifecycleEvent> getSettlementStatusHistory(
            Long settlementId) {

        getSettlementById(settlementId);

        return settlementLifecycleService.getStatusHistory(settlementId);
    }

    @Transactional(readOnly = true)
    public List<SettlementAuditEvent> getSettlementAuditHistory(Long settlementId) {
        getSettlementById(settlementId);
        return settlementAuditService.getSettlementAuditHistory(settlementId);
    }
}