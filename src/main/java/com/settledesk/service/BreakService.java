package com.settledesk.service;

import com.settledesk.dto.BreakResponse;
import com.settledesk.model.*;
import com.settledesk.repository.ReconciliationBreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}