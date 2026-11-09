
package com.settledesk.controller;

import com.settledesk.dto.ConfirmSettlementRequest;
import com.settledesk.dto.CreateSettlementRequest;
import com.settledesk.model.SettlementInstruction;
import com.settledesk.model.SettlementOutcome;
import com.settledesk.model.SettlementStatus;
import com.settledesk.model.Trade;
import com.settledesk.service.SettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettlementController.class)
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementService settlementService;

    private SettlementInstruction sampleSettlement() {
        Trade trade = Trade.builder()
                .tradeId(1L)
                .instrument("NIFTY")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("25000"))
                .buyer("Buyer A")
                .seller("Seller A")
                .build();

        return SettlementInstruction.builder()
                .settlementId(1L)
                .trade(trade)
                .settlementReference("SET-001")
                .settlementDate(LocalDate.of(2026, 11, 10))
                .status(SettlementStatus.CREATED)
                .createdAt(LocalDateTime.of(2026, 11, 9, 10, 0))
                .build();
    }

    @Test
    void createSettlement_shouldReturn201ForValidRequest() throws Exception {
        when(settlementService.createSettlementInstruction(any(CreateSettlementRequest.class)))
                .thenReturn(sampleSettlement());

        mockMvc.perform(post("/api/v1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeId": 1,
                                  "settlementDate": "2026-11-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.settlementId").value(1))
                .andExpect(jsonPath("$.settlementReference").value("SET-001"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createSettlement_shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeId": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    @Test
    void getAllSettlements_shouldReturn200() throws Exception {
        when(settlementService.getAllSettlements())
                .thenReturn(List.of(sampleSettlement()));

        mockMvc.perform(get("/api/v1/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].settlementId").value(1));
    }

    @Test
    void getSettlementById_shouldReturn200WhenFound() throws Exception {
        when(settlementService.getSettlementById(1L))
                .thenReturn(sampleSettlement());

        mockMvc.perform(get("/api/v1/settlements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementReference").value("SET-001"));
    }

    @Test
    void submitSettlement_shouldReturn200() throws Exception {
        SettlementInstruction submitted = sampleSettlement();
        submitted.setStatus(SettlementStatus.SUBMITTED);

        when(settlementService.submitSettlement(1L)).thenReturn(submitted);

        mockMvc.perform(post("/api/v1/settlements/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void confirmSettlement_shouldReturn200ForValidOutcome() throws Exception {
        SettlementInstruction settled = sampleSettlement();
        settled.setStatus(SettlementStatus.SETTLED);

        when(settlementService.confirmSettlement(
                eq(1L), any(ConfirmSettlementRequest.class)))
                .thenReturn(settled);

        mockMvc.perform(post("/api/v1/settlements/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "outcome": "SETTLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));
    }

    @Test
    void confirmSettlement_shouldReturn400WhenOutcomeIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/settlements/1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "failureReason": "Bank rejection"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(settlementService);
    }

    @Test
    void retrySettlement_shouldReturn200() throws Exception {
        SettlementInstruction retry = sampleSettlement();
        retry.setStatus(SettlementStatus.RETRY_PENDING);

        when(settlementService.retrySettlement(1L)).thenReturn(retry);

        mockMvc.perform(post("/api/v1/settlements/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETRY_PENDING"));
    }

    @Test
    void getStatusHistory_shouldReturn200() throws Exception {
        when(settlementService.getSettlementStatusHistory(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/settlements/1/status-history"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
