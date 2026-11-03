package com.settledesk.service;

import com.settledesk.dto.*;
import com.settledesk.model.*;
import com.settledesk.repository.ReconciliationBreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BreakService {

    private final ReconciliationBreakRepository breakRepository;

    @Transactional(readOnly = true)
    public Page<BreakResponse> searchBreaks(
            BreakStatus status,
            BreakCategory category,
            Long runId,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return breakRepository.searchBreaks(
                status, category, runId, pageable
        ).map(this::toResponse);
    }

    private BreakResponse toResponse(ReconciliationBreak b) {

        ReconciliationResult result = b.getResult();

        String reference = result.getExternalSettlementReference() != null
                ? result.getExternalSettlementReference()
                : result.getInternalSettlement() != null
                  ? result.getInternalSettlement().getSettlementReference()
                  : null;

        return BreakResponse.builder()
                .breakId(b.getBreakId())
                .resultId(result.getResultId())
                .runId(result.getRun().getRunId())
                .settlementReference(reference)
                .category(b.getCategory())
                .status(b.getStatus())
                .priority(b.getPriority())
                .description(b.getDescription())
                .assignedTo(b.getAssignedTo())
                .resolutionNotes(b.getResolutionNotes())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .resolvedAt(b.getResolvedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public BreakResponse getBreakById(Long breakId) {

        ReconciliationBreak reconciliationBreak =
                breakRepository.findById(breakId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Break not found with ID: " + breakId
                        ));

        return toResponse(reconciliationBreak);
    }

    @Transactional
    public BreakResponse updateBreak(
            Long breakId,
            UpdateBreakRequest request) {

        ReconciliationBreak reconciliationBreak =
                breakRepository.findById(breakId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Break not found with ID: " + breakId
                        ));

        if (request.getPriority() == null
                && request.getDescription() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one field must be provided for update"
            );
        }

        if (reconciliationBreak.getStatus() == BreakStatus.RESOLVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Resolved breaks cannot be updated"
            );
        }

        if (request.getPriority() != null) {
            reconciliationBreak.setPriority(request.getPriority());
        }

        if (request.getDescription() != null) {
            reconciliationBreak.setDescription(request.getDescription());
        }

        ReconciliationBreak updatedBreak =
                breakRepository.save(reconciliationBreak);

        return toResponse(updatedBreak);
    }


    @Transactional
    public BreakResponse resolveBreak(
            Long breakId,
            ResolveBreakRequest request) {

        ReconciliationBreak reconciliationBreak =
                breakRepository.findById(breakId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Break not found with ID: " + breakId
                        ));

        if (reconciliationBreak.getStatus() == BreakStatus.RESOLVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Break is already resolved"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        reconciliationBreak.setStatus(BreakStatus.RESOLVED);
        reconciliationBreak.setResolutionNotes(
                request.getResolutionNotes()
        );
        reconciliationBreak.setResolvedAt(now);
        reconciliationBreak.setUpdatedAt(now);

        ReconciliationBreak savedBreak =
                breakRepository.save(reconciliationBreak);

        return toResponse(savedBreak);
    }

    @Transactional
    public BreakResponse assignBreak(
            Long breakId,
            AssignBreakRequest request) {

        ReconciliationBreak reconciliationBreak =
                breakRepository.findById(breakId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Break not found with ID: " + breakId
                        ));

        if (reconciliationBreak.getStatus() == BreakStatus.RESOLVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Resolved breaks cannot be assigned"
            );
        }

        reconciliationBreak.setAssignedTo(request.getAssignedTo());
        reconciliationBreak.setStatus(BreakStatus.ASSIGNED);
        reconciliationBreak.setUpdatedAt(LocalDateTime.now());

        ReconciliationBreak savedBreak =
                breakRepository.save(reconciliationBreak);

        return toResponse(savedBreak);
    }

    @Transactional(readOnly = true)
    public BreakSummaryResponse getBreakSummary() {

        BreakSummaryResponse summary = new BreakSummaryResponse();

        summary.setTotalBreaks(breakRepository.count());

        summary.setOpenCount(
                breakRepository.countByStatus(BreakStatus.OPEN)
        );

        summary.setAssignedCount(
                breakRepository.countByStatus(BreakStatus.ASSIGNED)
        );

        summary.setResolvedCount(
                breakRepository.countByStatus(BreakStatus.RESOLVED)
        );

        summary.setMismatchCount(
                breakRepository.countByCategory(BreakCategory.MISMATCH)
        );

        summary.setMissingExternalCount(
                breakRepository.countByCategory(BreakCategory.MISSING_EXTERNAL)
        );

        summary.setMissingInternalCount(
                breakRepository.countByCategory(BreakCategory.MISSING_INTERNAL)
        );

        return summary;
    }

}