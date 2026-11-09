
package com.settledesk.controller;

import com.settledesk.dto.CreateTradeRequest;
import com.settledesk.model.Trade;
import com.settledesk.service.TradeAuditService;
import com.settledesk.service.TradeLifecycleService;
import com.settledesk.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeController.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeService tradeService;

    @MockitoBean
    private TradeLifecycleService tradeLifecycleService;

    @MockitoBean
    private TradeAuditService tradeAuditService;

    private Trade sampleTrade() {
        return Trade.builder()
                .tradeId(1L)
                .instrument("NIFTY")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("25000"))
                .buyer("Buyer A")
                .seller("Seller A")
                .build();
    }

    @Test
    void createTrade_shouldReturn201WhenRequestIsValid() throws Exception {
        when(tradeService.createTrade(any(CreateTradeRequest.class)))
                .thenReturn(sampleTrade());

        mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "instrument": "NIFTY",
                                  "quantity": 10,
                                  "price": 25000,
                                  "buyer": "Buyer A",
                                  "seller": "Seller A"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tradeId").value(1))
                .andExpect(jsonPath("$.instrument").value("NIFTY"));

        verify(tradeService).createTrade(any(CreateTradeRequest.class));
    }

    @Test
    void createTrade_shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "instrument": "",
                                  "quantity": -10,
                                  "price": 0,
                                  "buyer": "",
                                  "seller": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tradeService);
    }

    @Test
    void getTradeById_shouldReturn200WhenTradeExists() throws Exception {
        when(tradeService.getTradeByTradeId(1L)).thenReturn(sampleTrade());

        mockMvc.perform(get("/api/v1/trades/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(1))
                .andExpect(jsonPath("$.instrument").value("NIFTY"));
    }

    @Test
    void searchTrades_shouldReturn400ForInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest());

        verify(tradeService, never())
                .searchTrades(any(), any(), any());
    }

    @Test
    void searchTrades_shouldReturn400ForInvalidDirection() throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .param("direction", "random"))
                .andExpect(status().isBadRequest());

        verify(tradeService, never())
                .searchTrades(any(), any(), any());
    }

    @Test
    void searchTrades_shouldReturn400ForInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/trades")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(tradeService, never())
                .searchTrades(any(), any(), any());
    }

    @Test
    void searchTrades_shouldReturn200ForValidRequest() throws Exception {
        when(tradeService.searchTrades(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleTrade())));

        mockMvc.perform(get("/api/v1/trades")
                        .param("instrument", "NIFTY")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "tradeId")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tradeId").value(1))
                .andExpect(jsonPath("$.content[0].instrument").value("NIFTY"));
    }
}
