
package com.settledesk.service;

import com.settledesk.dto.CreateTradeConfirmationRequest;
import com.settledesk.event.TradeConfirmedEvent;
import com.settledesk.model.Trade;
import com.settledesk.model.TradeConfirmation;
import com.settledesk.model.TradeStatus;
import com.settledesk.repository.TradeConfirmationRepository;
import com.settledesk.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeConfirmationServiceTest {

    @Mock
    private TradeConfirmationRepository tradeConfirmationRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TradeConfirmationService tradeConfirmationService;

    @Test
    void createConfirmation_shouldSaveAndPublishEvent() {

        Trade trade = Trade.builder()
                .tradeId(701L)
                .instrument("AAPL")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("210.50"))
                .buyer("Buyer A")
                .seller("Seller A")
                .status(TradeStatus.NEW)
                .build();

        CreateTradeConfirmationRequest request =
                new CreateTradeConfirmationRequest();

        request.setCounterpartyReference("CP-REF-701");
        request.setInstrument("AAPL");
        request.setQuantity(new BigDecimal("100"));
        request.setPrice(new BigDecimal("210.50"));
        request.setBuyer("Buyer A");
        request.setSeller("Seller A");

        when(tradeRepository.findById(701L))
                .thenReturn(Optional.of(trade));

        when(tradeConfirmationRepository.existsByTrade_TradeId(701L))
                .thenReturn(false);

        when(tradeConfirmationRepository.save(any(TradeConfirmation.class)))
                .thenAnswer(invocation -> {
                    TradeConfirmation confirmation = invocation.getArgument(0);
                    confirmation.setConfirmationId(1701L);
                    return confirmation;
                });

        TradeConfirmation result =
                tradeConfirmationService.createConfirmation(701L, request);

        assertThat(result.getConfirmationId()).isEqualTo(1701L);
        assertThat(result.getTrade()).isSameAs(trade);
        assertThat(result.getCounterpartyReference())
                .isEqualTo("CP-REF-701");
        assertThat(result.getInstrument()).isEqualTo("AAPL");
        assertThat(result.getQuantity()).isEqualByComparingTo("100");
        assertThat(result.getPrice()).isEqualByComparingTo("210.50");

        ArgumentCaptor<TradeConfirmedEvent> eventCaptor =
                ArgumentCaptor.forClass(TradeConfirmedEvent.class);

        verify(outboxService)
                .saveTradeConfirmedEvent(eventCaptor.capture());

        TradeConfirmedEvent event = eventCaptor.getValue();

        assertThat(event.confirmationId()).isEqualTo(1701L);
        assertThat(event.tradeId()).isEqualTo(701L);
        assertThat(event.counterpartyReference())
                .isEqualTo("CP-REF-701");
        assertThat(event.instrument()).isEqualTo("AAPL");
        assertThat(event.receivedAt()).isEqualTo(result.getReceivedAt());
    }

    @Test
    void createConfirmation_shouldThrowNotFoundWhenTradeDoesNotExist() {

        CreateTradeConfirmationRequest request =
                new CreateTradeConfirmationRequest();

        when(tradeRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> tradeConfirmationService.createConfirmation(999L, request)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("Trade not found with id: 999");

        verify(tradeConfirmationRepository, never())
                .save(any(TradeConfirmation.class));

        verifyNoInteractions(outboxService);
    }

    @Test
    void createConfirmation_shouldRejectDuplicateConfirmation() {

        Trade trade = Trade.builder()
                .tradeId(702L)
                .status(TradeStatus.NEW)
                .build();

        CreateTradeConfirmationRequest request =
                new CreateTradeConfirmationRequest();

        when(tradeRepository.findById(702L))
                .thenReturn(Optional.of(trade));

        when(tradeConfirmationRepository.existsByTrade_TradeId(702L))
                .thenReturn(true);

        assertThatThrownBy(
                () -> tradeConfirmationService.createConfirmation(702L, request)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT")
                .hasMessageContaining(
                        "Confirmation already exists for trade: 702"
                );

        verify(tradeConfirmationRepository, never())
                .save(any(TradeConfirmation.class));

        verifyNoInteractions(outboxService);
    }

    @Test
    void getConfirmationByTradeId_shouldReturnConfirmationWhenFound() {

        Trade trade = Trade.builder()
                .tradeId(703L)
                .status(TradeStatus.NEW)
                .build();

        TradeConfirmation confirmation = TradeConfirmation.builder()
                .confirmationId(1703L)
                .trade(trade)
                .counterpartyReference("CP-REF-703")
                .instrument("MSFT")
                .quantity(new BigDecimal("200"))
                .price(new BigDecimal("420.75"))
                .buyer("Buyer B")
                .seller("Seller B")
                .receivedAt(LocalDateTime.now())
                .build();

        when(tradeRepository.existsById(703L)).thenReturn(true);

        when(tradeConfirmationRepository.findByTrade_TradeId(703L))
                .thenReturn(Optional.of(confirmation));

        TradeConfirmation result =
                tradeConfirmationService.getConfirmationByTradeId(703L);

        assertThat(result).isSameAs(confirmation);
        assertThat(result.getConfirmationId()).isEqualTo(1703L);
    }

    @Test
    void getConfirmationByTradeId_shouldThrowNotFoundWhenTradeDoesNotExist() {

        when(tradeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(
                () -> tradeConfirmationService.getConfirmationByTradeId(999L)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("Trade not found with id: 999");

        verifyNoInteractions(tradeConfirmationRepository);
    }

    @Test
    void getConfirmationByTradeId_shouldThrowNotFoundWhenConfirmationDoesNotExist() {

        when(tradeRepository.existsById(704L)).thenReturn(true);

        when(tradeConfirmationRepository.findByTrade_TradeId(704L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> tradeConfirmationService.getConfirmationByTradeId(704L)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining(
                        "Confirmation not found for trade: 704"
                );
    }
}
