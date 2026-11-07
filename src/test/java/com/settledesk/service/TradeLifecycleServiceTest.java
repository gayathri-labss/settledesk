
package com.settledesk.service;

import com.settledesk.model.*;
import com.settledesk.repository.TradeLifecycleRepository;
import com.settledesk.repository.TradeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeLifecycleServiceTest {

    @Mock
    private TradeLifecycleRepository lifecycleRepository;

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeLifecycleService tradeLifecycleService;

    private Trade trade;

    @BeforeEach
    void setUp() {
        trade = Trade.builder()
                .tradeId(70001L)
                .instrument("AAPL")
                .quantity(new java.math.BigDecimal("100"))
                .price(new java.math.BigDecimal("210.50"))
                .status(TradeStatus.MATCHED)
                .build();
    }

    @Test
    void recordStatusChange_shouldSaveLifecycleEvent() {
        tradeLifecycleService.recordStatusChange(
                trade,
                TradeStatus.NEW,
                TradeStatus.MATCHED
        );

        verify(lifecycleRepository).save(argThat(event ->
                event.getTrade() == trade
                        && event.getPreviousStatus() == TradeStatus.NEW
                        && event.getNewStatus() == TradeStatus.MATCHED
                        && event.getChangedAt() != null
        ));
    }

    @Test
    void getTradeLifecycle_shouldReturnHistoryWhenTradeExists() {
        TradeLifecycleEvent firstEvent = TradeLifecycleEvent.builder()
                .trade(trade)
                .previousStatus(null)
                .newStatus(TradeStatus.NEW)
                .build();

        TradeLifecycleEvent secondEvent = TradeLifecycleEvent.builder()
                .trade(trade)
                .previousStatus(TradeStatus.NEW)
                .newStatus(TradeStatus.MATCHED)
                .build();

        List<TradeLifecycleEvent> history =
                List.of(firstEvent, secondEvent);

        when(tradeRepository.existsById(70001L)).thenReturn(true);

        when(lifecycleRepository
                .findByTrade_TradeIdOrderByChangedAtAsc(70001L))
                .thenReturn(history);

        List<TradeLifecycleEvent> result =
                tradeLifecycleService.getTradeLifecycle(70001L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNewStatus())
                .isEqualTo(TradeStatus.NEW);
        assertThat(result.get(1).getPreviousStatus())
                .isEqualTo(TradeStatus.NEW);
        assertThat(result.get(1).getNewStatus())
                .isEqualTo(TradeStatus.MATCHED);

        verify(tradeRepository).existsById(70001L);
        verify(lifecycleRepository)
                .findByTrade_TradeIdOrderByChangedAtAsc(70001L);
    }

    @Test
    void getTradeLifecycle_shouldThrowNotFoundWhenTradeDoesNotExist() {
        when(tradeRepository.existsById(70001L)).thenReturn(false);

        assertThatThrownBy(() ->
                tradeLifecycleService.getTradeLifecycle(70001L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(lifecycleRepository, never())
                .findByTrade_TradeIdOrderByChangedAtAsc(any());
    }
}
