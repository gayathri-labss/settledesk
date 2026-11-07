
package com.settledesk.service;

import com.settledesk.dto.MatchingRunRequest;
import com.settledesk.event.TradeMatchedEvent;
import com.settledesk.event.TradeMatchingFailedEvent;
import com.settledesk.model.*;
import com.settledesk.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private MatchingRunRepository matchingRunRepository;

    @Mock
    private MatchingResultRepository matchingResultRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeConfirmationRepository tradeConfirmationRepository;

    @Mock
    private TradeService tradeService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private MatchingService matchingService;

    private Trade createTrade(Long id) {
        return Trade.builder()
                .tradeId(id)
                .instrument("AAPL")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("210.50"))
                .buyer("Buyer A")
                .seller("Seller A")
                .status(TradeStatus.NEW)
                .build();
    }

    private TradeConfirmation createConfirmation(Trade trade) {
        return TradeConfirmation.builder()
                .confirmationId(1001L)
                .trade(trade)
                .counterpartyReference("CP-REF")
                .instrument("AAPL")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("210.50"))
                .buyer("Buyer A")
                .seller("Seller A")
                .build();
    }

    @Test
    void startMatchingRun_shouldMatchTradeAndPublishEvent() {

        Trade trade = createTrade(801L);
        TradeConfirmation confirmation = createConfirmation(trade);

        MatchingRunRequest request = new MatchingRunRequest();
        request.setTradeIds(List.of(801L));

        when(tradeRepository.findById(801L))
                .thenReturn(Optional.of(trade));

        when(matchingRunRepository.save(any(MatchingRun.class)))
                .thenAnswer(invocation -> {
                    MatchingRun run = invocation.getArgument(0);
                    run.setRunId(501L);
                    return run;
                });

        when(tradeConfirmationRepository.findByTrade_TradeId(801L))
                .thenReturn(Optional.of(confirmation));

        MatchingRun result = matchingService.startMatchingRun(request);

        assertThat(result.getRunId()).isEqualTo(501L);
        assertThat(result.getStatus()).isEqualTo(MatchingRunStatus.COMPLETED);
        assertThat(result.getTotalTrades()).isEqualTo(1);
        assertThat(result.getMatchedCount()).isEqualTo(1);
        assertThat(result.getUnmatchedCount()).isZero();

        ArgumentCaptor<MatchingResult> resultCaptor =
                ArgumentCaptor.forClass(MatchingResult.class);

        verify(matchingResultRepository).save(resultCaptor.capture());

        assertThat(resultCaptor.getValue().getOutcome())
                .isEqualTo(MatchingOutcome.MATCHED);

        verify(tradeService).updateTradeStatus(
                eq(801L),
                any(com.settledesk.dto.UpdateTradeStatusRequest.class)
        );

        verify(outboxService).saveTradeMatchedEvent(
                any(TradeMatchedEvent.class)
        );

        verify(outboxService, never())
                .saveTradeMatchingFailedEvent(any(TradeMatchingFailedEvent.class));
    }

    @Test
    void startMatchingRun_shouldMarkTradeUnmatchedWhenConfirmationMissing() {

        Trade trade = createTrade(802L);

        MatchingRunRequest request = new MatchingRunRequest();
        request.setTradeIds(List.of(802L));

        when(tradeRepository.findById(802L))
                .thenReturn(Optional.of(trade));

        when(matchingRunRepository.save(any(MatchingRun.class)))
                .thenAnswer(invocation -> {
                    MatchingRun run = invocation.getArgument(0);
                    run.setRunId(502L);
                    return run;
                });

        when(tradeConfirmationRepository.findByTrade_TradeId(802L))
                .thenReturn(Optional.empty());

        MatchingRun result = matchingService.startMatchingRun(request);

        assertThat(result.getStatus()).isEqualTo(MatchingRunStatus.COMPLETED);
        assertThat(result.getMatchedCount()).isZero();
        assertThat(result.getUnmatchedCount()).isEqualTo(1);

        ArgumentCaptor<MatchingResult> resultCaptor =
                ArgumentCaptor.forClass(MatchingResult.class);

        verify(matchingResultRepository).save(resultCaptor.capture());

        assertThat(resultCaptor.getValue().getOutcome())
                .isEqualTo(MatchingOutcome.UNMATCHED);

        assertThat(resultCaptor.getValue().getMismatchDetails())
                .contains("No counterparty confirmation found");

        verify(outboxService).saveTradeMatchingFailedEvent(
                any(TradeMatchingFailedEvent.class)
        );

        verify(tradeService, never()).updateTradeStatus(
                anyLong(),
                any(com.settledesk.dto.UpdateTradeStatusRequest.class)
        );
    }

    @Test
    void startMatchingRun_shouldRejectDuplicateTradeIds() {

        MatchingRunRequest request = new MatchingRunRequest();
        request.setTradeIds(List.of(801L, 801L));

        assertThatThrownBy(() -> matchingService.startMatchingRun(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("Duplicate trade IDs are not allowed");

        verifyNoInteractions(
                matchingRunRepository,
                matchingResultRepository,
                tradeRepository,
                tradeConfirmationRepository,
                tradeService,
                outboxService
        );
    }

    @Test
    void startMatchingRun_shouldThrowNotFoundWhenTradeDoesNotExist() {

        MatchingRunRequest request = new MatchingRunRequest();
        request.setTradeIds(List.of(999L));

        when(tradeRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.startMatchingRun(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("Trade not found with id: 999");

        verifyNoInteractions(
                matchingRunRepository,
                matchingResultRepository,
                outboxService
        );
    }
}
