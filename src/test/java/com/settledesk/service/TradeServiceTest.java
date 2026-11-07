
package com.settledesk.service;

import com.settledesk.dto.CreateTradeRequest;
import com.settledesk.event.TradeCreatedEvent;
import com.settledesk.model.Trade;
import com.settledesk.model.TradeAuditAction;
import com.settledesk.model.TradeStatus;
import com.settledesk.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;

import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import com.settledesk.dto.UpdateTradeStatusRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeLifecycleService tradeLifecycleService;

    @Mock
    private TradeAuditService tradeAuditService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TradeService tradeService;

    @Test
    void createTrade_shouldSaveTradeAndPublishEvent() {


        CreateTradeRequest request = new CreateTradeRequest();
        request.setInstrument("AAPL");
        request.setQuantity(new BigDecimal("100"));
        request.setPrice(new BigDecimal("210.50"));
        request.setBuyer("Buyer A");
        request.setSeller("Seller B");

        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> {
                    Trade trade = invocation.getArgument(0);
                    trade.setTradeId(101L);
                    return trade;
                });

        Trade result = tradeService.createTrade(request);

        assertThat(result.getTradeId()).isEqualTo(101L);
        assertThat(result.getInstrument()).isEqualTo("AAPL");
        assertThat(result.getQuantity()).isEqualByComparingTo("100");
        assertThat(result.getPrice()).isEqualByComparingTo("210.50");
        assertThat(result.getStatus()).isEqualTo(TradeStatus.NEW);

        ArgumentCaptor<TradeCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(TradeCreatedEvent.class);

        verify(outboxService)
                .saveTradeCreatedEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue())
                .isEqualTo(new TradeCreatedEvent(
                        101L,
                        "AAPL",
                        new BigDecimal("100"),
                        new BigDecimal("210.50"),
                        "Buyer A",
                        "Seller B",
                        "NEW"
                ));

        verify(tradeLifecycleService)
                .recordStatusChange(result, null, TradeStatus.NEW);

        verify(tradeAuditService)
                .recordAudit(
                        result,
                        TradeAuditAction.CREATE,
                        "Trade created"
                );
    }


    @Test
    void createTrades_shouldSaveEachTradeAndPublishEvent() {

        CreateTradeRequest request1 = new CreateTradeRequest();
        request1.setInstrument("AAPL");
        request1.setQuantity(new BigDecimal("100"));
        request1.setPrice(new BigDecimal("210.50"));
        request1.setBuyer("Buyer A");
        request1.setSeller("Seller A");

        CreateTradeRequest request2 = new CreateTradeRequest();
        request2.setInstrument("MSFT");
        request2.setQuantity(new BigDecimal("200"));
        request2.setPrice(new BigDecimal("420.75"));
        request2.setBuyer("Buyer B");
        request2.setSeller("Seller B");

        when(tradeRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<Trade> trades = invocation.getArgument(0);

                    long id = 201L;
                    for (Trade trade : trades) {
                        trade.setTradeId(id++);
                    }

                    return trades;
                });

        List<Trade> result = tradeService.createTrades(
                List.of(request1, request2)
        );

        // Assert: both trades were saved
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTradeId()).isEqualTo(201L);
        assertThat(result.get(1).getTradeId()).isEqualTo(202L);

        // Assert: one event per trade
        ArgumentCaptor<TradeCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(TradeCreatedEvent.class);

        verify(outboxService, times(2))
                .saveTradeCreatedEvent(eventCaptor.capture());

        assertThat(eventCaptor.getAllValues())
                .extracting(TradeCreatedEvent::tradeId)
                .containsExactly(201L, 202L);

        // Assert: lifecycle and audit recorded for both trades
        verify(tradeLifecycleService, times(2))
                .recordStatusChange(
                        any(Trade.class),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.eq(TradeStatus.NEW)
                );

        verify(tradeAuditService, times(2))
                .recordAudit(
                        any(Trade.class),
                        org.mockito.ArgumentMatchers.eq(TradeAuditAction.CREATE),
                        any(String.class)
                );
    }


    @Test
    void getTradeByTradeId_shouldReturnTradeWhenFound() {

        Trade trade = Trade.builder()
                .tradeId(301L)
                .instrument("AAPL")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("210.50"))
                .buyer("Buyer A")
                .seller("Seller A")
                .status(TradeStatus.NEW)
                .build();

        when(tradeRepository.findById(301L))
                .thenReturn(Optional.of(trade));

        Trade result = tradeService.getTradeByTradeId(301L);

        assertThat(result).isSameAs(trade);

        verify(tradeRepository).findById(301L);
    }

    @Test
    void getTradeByTradeId_shouldThrowNotFoundWhenTradeDoesNotExist() {

        when(tradeRepository.findById(999L))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> tradeService.getTradeByTradeId(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("Trade not found with id: 999");

        verify(tradeRepository).findById(999L);
    }


    @Test
    void updateTradeStatus_shouldAllowValidTransition() {

        Trade trade = Trade.builder()
                .tradeId(401L)
                .instrument("AAPL")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("210.50"))
                .buyer("Buyer A")
                .seller("Seller A")
                .status(TradeStatus.NEW)
                .build();

        UpdateTradeStatusRequest request = new UpdateTradeStatusRequest();
        request.setStatus(TradeStatus.MATCHED);

        when(tradeRepository.findById(401L))
                .thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Trade result = tradeService.updateTradeStatus(401L, request);

        assertThat(result.getStatus()).isEqualTo(TradeStatus.MATCHED);

        verify(tradeRepository).save(trade);
        verify(tradeLifecycleService)
                .recordStatusChange(trade, TradeStatus.NEW, TradeStatus.MATCHED);
        verify(tradeAuditService)
                .recordAudit(
                        trade,
                        TradeAuditAction.STATUS_CHANGE,
                        "Trade status changed from NEW to MATCHED"
                );
    }

    @Test
    void updateTradeStatus_shouldAllowMatchedToConfirmed() {

        Trade trade = Trade.builder()
                .tradeId(402L)
                .instrument("MSFT")
                .quantity(new BigDecimal("200"))
                .price(new BigDecimal("420.75"))
                .buyer("Buyer B")
                .seller("Seller B")
                .status(TradeStatus.MATCHED)
                .build();

        UpdateTradeStatusRequest request = new UpdateTradeStatusRequest();
        request.setStatus(TradeStatus.CONFIRMED);

        when(tradeRepository.findById(402L))
                .thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Trade result = tradeService.updateTradeStatus(402L, request);

        assertThat(result.getStatus()).isEqualTo(TradeStatus.CONFIRMED);

        verify(tradeLifecycleService)
                .recordStatusChange(
                        trade,
                        TradeStatus.MATCHED,
                        TradeStatus.CONFIRMED
                );
        verify(tradeAuditService)
                .recordAudit(
                        trade,
                        TradeAuditAction.STATUS_CHANGE,
                        "Trade status changed from MATCHED to CONFIRMED"
                );
    }

    @Test
    void updateTradeStatus_shouldRejectInvalidTransition() {

        Trade trade = Trade.builder()
                .tradeId(403L)
                .instrument("GOOGL")
                .quantity(new BigDecimal("150"))
                .price(new BigDecimal("180.25"))
                .buyer("Buyer C")
                .seller("Seller C")
                .status(TradeStatus.NEW)
                .build();

        UpdateTradeStatusRequest request = new UpdateTradeStatusRequest();
        request.setStatus(TradeStatus.SETTLED);

        when(tradeRepository.findById(403L))
                .thenReturn(Optional.of(trade));

        assertThatThrownBy(
                () -> tradeService.updateTradeStatus(403L, request)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT")
                .hasMessageContaining(
                        "Invalid trade status transition from NEW to SETTLED"
                );

        verify(tradeRepository, never()).save(any(Trade.class));
        verifyNoInteractions(tradeLifecycleService, tradeAuditService);
    }


    @Test
    void cancelTrade_shouldCancelTradeWhenTransitionIsValid() {

        Trade trade = Trade.builder()
                .tradeId(501L)
                .instrument("AAPL")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("210.50"))
                .buyer("Buyer A")
                .seller("Seller A")
                .status(TradeStatus.NEW)
                .build();

        when(tradeRepository.findById(501L))
                .thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Trade result = tradeService.cancelTrade(501L);

        assertThat(result.getStatus()).isEqualTo(TradeStatus.CANCELLED);

        verify(tradeLifecycleService)
                .recordStatusChange(
                        trade,
                        TradeStatus.NEW,
                        TradeStatus.CANCELLED
                );

        verify(tradeAuditService)
                .recordAudit(
                        trade,
                        TradeAuditAction.CANCEL,
                        "Trade cancelled from status: NEW"
                );
    }

    @Test
    void cancelTrade_shouldRejectCancellationOfSettledTrade() {

        Trade trade = Trade.builder()
                .tradeId(502L)
                .instrument("MSFT")
                .quantity(new BigDecimal("200"))
                .price(new BigDecimal("420.75"))
                .buyer("Buyer B")
                .seller("Seller B")
                .status(TradeStatus.SETTLED)
                .build();

        when(tradeRepository.findById(502L))
                .thenReturn(Optional.of(trade));

        assertThatThrownBy(() -> tradeService.cancelTrade(502L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT")
                .hasMessageContaining(
                        "Trade cannot be cancelled from status: SETTLED"
                );

        verify(tradeRepository, never()).save(any(Trade.class));
        verifyNoInteractions(tradeLifecycleService, tradeAuditService);
    }




}
