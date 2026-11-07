
package com.settledesk.service;

import com.settledesk.dto.*;
import com.settledesk.model.*;
import com.settledesk.repository.ReconciliationBreakRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BreakServiceTest {

    @Mock
    private ReconciliationBreakRepository breakRepository;

    @InjectMocks
    private BreakService breakService;

    private ReconciliationBreak reconciliationBreak;

    @BeforeEach
    void setUp() {
        ReconciliationRun run = ReconciliationRun.builder()
                .runId(50001L)
                .build();

        ReconciliationResult result = ReconciliationResult.builder()
                .resultId(60001L)
                .run(run)
                .externalSettlementReference("SET-10001")
                .build();

        reconciliationBreak = ReconciliationBreak.builder()
                .breakId(70001L)
                .result(result)
                .category(BreakCategory.MISMATCH)
                .status(BreakStatus.OPEN)
                .priority(BreakPriority.MEDIUM)
                .description("Price mismatch")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void searchBreaks_shouldReturnMappedBreakResponses() {
        Page<ReconciliationBreak> page =
                new PageImpl<>(List.of(reconciliationBreak));

        when(breakRepository.searchBreaks(
                eq(BreakStatus.OPEN),
                eq(BreakCategory.MISMATCH),
                eq(50001L),
                any(Pageable.class)))
                .thenReturn(page);

        Page<BreakResponse> result = breakService.searchBreaks(
                BreakStatus.OPEN,
                BreakCategory.MISMATCH,
                50001L,
                0,
                10
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getBreakId())
                .isEqualTo(70001L);
        assertThat(result.getContent().get(0).getSettlementReference())
                .isEqualTo("SET-10001");

        verify(breakRepository).searchBreaks(
                eq(BreakStatus.OPEN),
                eq(BreakCategory.MISMATCH),
                eq(50001L),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                                && pageable.getSort().getOrderFor("createdAt")
                                .getDirection() == Sort.Direction.DESC)
        );
    }

    @Test
    void getBreakById_shouldReturnBreakWhenFound() {
        when(breakRepository.findById(70001L))
                .thenReturn(Optional.of(reconciliationBreak));

        BreakResponse result = breakService.getBreakById(70001L);

        assertThat(result.getBreakId()).isEqualTo(70001L);
        assertThat(result.getCategory()).isEqualTo(BreakCategory.MISMATCH);
        assertThat(result.getStatus()).isEqualTo(BreakStatus.OPEN);
    }

    @Test
    void getBreakById_shouldThrowNotFoundWhenMissing() {
        when(breakRepository.findById(70001L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> breakService.getBreakById(70001L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateBreak_shouldUpdatePriorityAndDescription() {
        UpdateBreakRequest request = new UpdateBreakRequest();
        request.setPriority(BreakPriority.HIGH);
        request.setDescription("Updated price mismatch");

        when(breakRepository.findById(70001L))
                .thenReturn(Optional.of(reconciliationBreak));
        when(breakRepository.save(any(ReconciliationBreak.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BreakResponse result =
                breakService.updateBreak(70001L, request);

        assertThat(result.getPriority()).isEqualTo(BreakPriority.HIGH);
        assertThat(result.getDescription())
                .isEqualTo("Updated price mismatch");

        verify(breakRepository).save(reconciliationBreak);
    }

    @Test
    void updateBreak_shouldRejectEmptyUpdate() {
        UpdateBreakRequest request = new UpdateBreakRequest();

        when(breakRepository.findById(70001L))
                .thenReturn(Optional.of(reconciliationBreak));

        assertThatThrownBy(() ->
                breakService.updateBreak(70001L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(breakRepository, never()).save(any());
    }

    @Test
    void updateBreak_shouldRejectUpdateOfResolvedBreak() {
        reconciliationBreak.setStatus(BreakStatus.RESOLVED);

        UpdateBreakRequest request = new UpdateBreakRequest();
        request.setPriority(BreakPriority.HIGH);

        when(breakRepository.findById(70001L))
                .thenReturn(Optional.of(reconciliationBreak));

        assertThatThrownBy(() ->
                breakService.updateBreak(70001L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(breakRepository, never()).save(any());
    }

    @Test
    void resolveBreak_shouldResolveOpenBreak() {
        ResolveBreakRequest request = new ResolveBreakRequest();
        request.setResolutionNotes("Price discrepancy corrected");

        when(breakRepository.findById(70001L))
                .thenReturn(Optional.of(reconciliationBreak));
        when(breakRepository.save(any(ReconciliationBreak.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BreakResponse result =
                breakService.resolveBreak(70001L, request);

        assertThat(result.getStatus()).isEqualTo(BreakStatus.RESOLVED);
        assertThat(result.getResolutionNotes())
                .isEqualTo("Price discrepancy corrected");
        assertThat(result.getResolvedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(breakRepository).save(reconciliationBreak);
    }

    @Test
    void resolveBreak_shouldRejectAlreadyResolvedBreak() {
        reconciliationBreak.setStatus(BreakStatus.RESOLVED);

        ResolveBreakRequest request = new ResolveBreakRequest();
        request.setResolutionNotes("Already resolved");

        when(breakRepository.findById(70001L))
                .thenReturn(Optional.of(reconciliationBreak));

        assertThatThrownBy(() ->
                breakService.resolveBreak(70001L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(breakRepository, never()).save(any());
    }

    @Test
    void assignBreak_shouldAssignOpenBreak() {
        AssignBreakRequest request = new AssignBreakRequest();
        request.setAssignedTo("operations-team");

        when(breakRepository.findById(70001L))
                .thenReturn(Optional.of(reconciliationBreak));
        when(breakRepository.save(any(ReconciliationBreak.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BreakResponse result =
                breakService.assignBreak(70001L, request);

        assertThat(result.getAssignedTo()).isEqualTo("operations-team");
        assertThat(result.getStatus()).isEqualTo(BreakStatus.ASSIGNED);
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(breakRepository).save(reconciliationBreak);
    }

    @Test
    void assignBreak_shouldRejectAlreadyResolvedBreak() {
        reconciliationBreak.setStatus(BreakStatus.RESOLVED);

        AssignBreakRequest request = new AssignBreakRequest();
        request.setAssignedTo("operations-team");

        when(breakRepository.findById(70001L))
                .thenReturn(Optional.of(reconciliationBreak));

        assertThatThrownBy(() ->
                breakService.assignBreak(70001L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(breakRepository, never()).save(any());
    }

    @Test
    void getBreakSummary_shouldReturnAllBreakCounts() {
        when(breakRepository.count()).thenReturn(10L);
        when(breakRepository.countByStatus(BreakStatus.OPEN))
                .thenReturn(4L);
        when(breakRepository.countByStatus(BreakStatus.ASSIGNED))
                .thenReturn(3L);
        when(breakRepository.countByStatus(BreakStatus.RESOLVED))
                .thenReturn(3L);
        when(breakRepository.countByCategory(BreakCategory.MISMATCH))
                .thenReturn(5L);
        when(breakRepository.countByCategory(BreakCategory.MISSING_EXTERNAL))
                .thenReturn(3L);
        when(breakRepository.countByCategory(BreakCategory.MISSING_INTERNAL))
                .thenReturn(2L);

        BreakSummaryResponse result =
                breakService.getBreakSummary();

        assertThat(result.getTotalBreaks()).isEqualTo(10L);
        assertThat(result.getOpenCount()).isEqualTo(4L);
        assertThat(result.getAssignedCount()).isEqualTo(3L);
        assertThat(result.getResolvedCount()).isEqualTo(3L);
        assertThat(result.getMismatchCount()).isEqualTo(5L);
        assertThat(result.getMissingExternalCount()).isEqualTo(3L);
        assertThat(result.getMissingInternalCount()).isEqualTo(2L);
    }
}
