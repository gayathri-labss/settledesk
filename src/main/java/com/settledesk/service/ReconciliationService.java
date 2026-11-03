package com.settledesk.service;

import com.settledesk.dto.ExternalSettlementInput;
import com.settledesk.dto.StartReconciliationRequest;
import com.settledesk.model.*;
import com.settledesk.repository.ReconciliationBreakRepository;
import com.settledesk.repository.ReconciliationResultRepository;
import com.settledesk.repository.ReconciliationRunRepository;
import com.settledesk.repository.SettlementRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final SettlementRepository settlementRepository;
    private final ReconciliationRunRepository runRepository;
    private final ReconciliationResultRepository resultRepository;
    private final ReconciliationBreakRepository breakRepository;

    @Transactional
    public ReconciliationRun startReconciliation(
            StartReconciliationRequest request) {

        LocalDateTime now = LocalDateTime.now();

        List<SettlementInstruction> internalRecords =
                settlementRepository.findBySettlementDate(
                        request.getSettlementDate());

        ReconciliationRun run = ReconciliationRun.builder()
                .source(request.getSource())
                .settlementDate(request.getSettlementDate())
                .status(ReconciliationRunStatus.IN_PROGRESS)
                .startedAt(now)
                .totalInternalRecords(internalRecords.size())
                .totalExternalRecords(request.getExternalRecords().size())
                .build();

        run = runRepository.save(run);

        Map<String, SettlementInstruction> internalMap =
                internalRecords.stream().collect(Collectors.toMap(
                        SettlementInstruction::getSettlementReference,
                        Function.identity()
                ));

        Set<String> matchedReferences = new HashSet<>();
        Set<String> externalReferences = new HashSet<>();
        Set<String> settlementReferences = new HashSet<>();

        List<ReconciliationResult> results = new ArrayList<>();

        for (ExternalSettlementInput external : request.getExternalRecords()) {

            if (!externalReferences.add(external.getExternalReference())
                    || !settlementReferences.add(
                    external.getSettlementReference())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate external reference or settlement reference"
                );
            }

            SettlementInstruction internal =
                    internalMap.get(external.getSettlementReference());

            ReconciliationResult result;

            if (internal == null) {
                result = buildResult(
                        run, null, external,
                        ReconciliationResultStatus.MISSING_INTERNAL,
                        "No internal settlement found"
                );
            } else {
                matchedReferences.add(internal.getSettlementReference());

                List<String> differences = new ArrayList<>();

                if (!internal.getTrade().getInstrument()
                        .equalsIgnoreCase(external.getInstrument())) {
                    differences.add("Instrument mismatch");
                }

                if (internal.getTrade().getQuantity()
                        .compareTo(external.getQuantity()) != 0) {
                    differences.add("Quantity mismatch");
                }

                if (internal.getTrade().getPrice()
                        .compareTo(external.getPrice()) != 0) {
                    differences.add("Price mismatch");
                }

                if (!internal.getSettlementDate()
                        .equals(external.getSettlementDate())) {
                    differences.add("Settlement date mismatch");
                }

                if (!internal.getStatus().name()
                        .equalsIgnoreCase(external.getStatus())) {
                    differences.add("Settlement status mismatch");
                }

                ReconciliationResultStatus status =
                        differences.isEmpty()
                                ? ReconciliationResultStatus.MATCHED
                                : ReconciliationResultStatus.MISMATCHED;

                result = buildResult(
                        run, internal, external, status,
                        String.join(", ", differences)
                );
            }

            results.add(result);
        }

        for (SettlementInstruction internal : internalRecords) {
            if (!matchedReferences.contains(
                    internal.getSettlementReference())) {

                results.add(buildResult(
                        run, internal, null,
                        ReconciliationResultStatus.MISSING_EXTERNAL,
                        "No external settlement record found"
                ));
            }
        }

        List<ReconciliationResult> savedResults =
                resultRepository.saveAll(results);

        List<ReconciliationBreak> breaks = results.stream()
                .filter(r -> r.getStatus() != ReconciliationResultStatus.MATCHED)
                .map(r -> ReconciliationBreak.builder()
                        .result(r)
                        .category(switch (r.getStatus()) {
                            case MISMATCHED -> BreakCategory.MISMATCH;
                            case MISSING_EXTERNAL -> BreakCategory.MISSING_EXTERNAL;
                            case MISSING_INTERNAL -> BreakCategory.MISSING_INTERNAL;
                            default -> throw new IllegalStateException(
                                    "Unexpected reconciliation status");
                        })
                        .status(BreakStatus.OPEN)
                        .priority(BreakPriority.MEDIUM)
                        .description(r.getMismatchDetails())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .toList();

        breakRepository.saveAll(breaks);

        run.setMatchedCount(count(results, ReconciliationResultStatus.MATCHED));
        run.setMismatchedCount(count(results, ReconciliationResultStatus.MISMATCHED));
        run.setMissingExternalCount(count(results, ReconciliationResultStatus.MISSING_EXTERNAL));
        run.setMissingInternalCount(count(results, ReconciliationResultStatus.MISSING_INTERNAL));

        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setCompletedAt(LocalDateTime.now());

        return runRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRun> getAllReconciliationRuns() {
        return runRepository.findAll();
    }

    private ReconciliationResult buildResult(
            ReconciliationRun run,
            SettlementInstruction internal,
            ExternalSettlementInput external,
            ReconciliationResultStatus status,
            String details) {

        return ReconciliationResult.builder()
                .run(run)
                .internalSettlement(internal)
                .externalReference(external == null ? null : external.getExternalReference())
                .externalSettlementReference(external == null ? null : external.getSettlementReference())
                .externalInstrument(external == null ? null : external.getInstrument())
                .externalQuantity(external == null ? null : external.getQuantity())
                .externalPrice(external == null ? null : external.getPrice())
                .externalSettlementDate(external == null ? null : external.getSettlementDate())
                .externalStatus(external == null ? null : external.getStatus())
                .status(status)
                .mismatchDetails(details)
                .reconciledAt(LocalDateTime.now())
                .build();
    }

    private int count(
            List<ReconciliationResult> results,
            ReconciliationResultStatus status) {
        return (int) results.stream()
                .filter(result -> result.getStatus() == status)
                .count();
    }

    @Transactional(readOnly = true)
    public ReconciliationRun getReconciliationRun(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reconciliation run not found: " + runId
                ));
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResult> getReconciliationResults(Long runId) {

        if (!runRepository.existsById(runId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reconciliation run not found: " + runId
            );
        }

        return resultRepository.findByRun_RunId(runId);
    }

    private ReconciliationBreak createBreak(ReconciliationResult result) {

        LocalDateTime now = LocalDateTime.now();

        BreakCategory category = switch (result.getStatus()) {
            case MISMATCHED -> BreakCategory.MISMATCH;
            case MISSING_EXTERNAL -> BreakCategory.MISSING_EXTERNAL;
            case MISSING_INTERNAL -> BreakCategory.MISSING_INTERNAL;
            case MATCHED -> throw new IllegalArgumentException(
                    "Cannot create a break for a matched result"
            );
        };

        return ReconciliationBreak.builder()
                .result(result)
                .category(category)
                .status(BreakStatus.OPEN)
                .priority(BreakPriority.MEDIUM)
                .description(result.getMismatchDetails())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
