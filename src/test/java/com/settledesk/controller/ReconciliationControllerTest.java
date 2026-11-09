
package com.settledesk.controller;

import com.settledesk.dto.StartReconciliationRequest;
import com.settledesk.model.ReconciliationRun;
import com.settledesk.model.ReconciliationRunStatus;
import com.settledesk.model.ReconciliationResult;
import com.settledesk.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReconciliationController.class)
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReconciliationService reconciliationService;

    private ReconciliationRun sampleRun() {
        return ReconciliationRun.builder()
                .runId(1L)
                .source("BANK_A")
                .settlementDate(LocalDate.of(2026, 11, 10))
                .status(ReconciliationRunStatus.COMPLETED)
                .startedAt(LocalDateTime.of(2026, 11, 9, 10, 0))
                .completedAt(LocalDateTime.of(2026, 11, 9, 10, 1))
                .totalInternalRecords(1)
                .totalExternalRecords(1)
                .matchedCount(1)
                .mismatchedCount(0)
                .missingExternalCount(0)
                .missingInternalCount(0)
                .build();
    }

    @Test
    void startReconciliation_shouldReturn201ForValidRequest() throws Exception {
        when(reconciliationService.startReconciliation(
                any(StartReconciliationRequest.class)))
                .thenReturn(sampleRun());

        mockMvc.perform(post("/api/v1/reconciliation/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "BANK_A",
                                  "settlementDate": "2026-11-10",
                                  "externalRecords": [
                                    {
                                      "externalReference": "EXT-001",
                                      "settlementReference": "SET-001",
                                      "instrument": "NIFTY",
                                      "quantity": 10,
                                      "price": 25000,
                                      "settlementDate": "2026-11-10",
                                      "status": "SETTLED"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(1))
                .andExpect(jsonPath("$.source").value("BANK_A"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.matchedCount").value(1));
    }

    @Test
    void startReconciliation_shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/reconciliation/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "",
                                  "settlementDate": null,
                                  "externalRecords": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reconciliationService);
    }

    @Test
    void startReconciliation_shouldReturn400ForInvalidExternalRecord() throws Exception {
        mockMvc.perform(post("/api/v1/reconciliation/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "BANK_A",
                                  "settlementDate": "2026-11-10",
                                  "externalRecords": [
                                    {
                                      "externalReference": "",
                                      "settlementReference": "SET-001",
                                      "instrument": "NIFTY",
                                      "quantity": -10,
                                      "price": 25000,
                                      "settlementDate": "2026-11-10",
                                      "status": "SETTLED"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reconciliationService);
    }

    @Test
    void getAllReconciliationRuns_shouldReturn200() throws Exception {
        when(reconciliationService.getAllReconciliationRuns())
                .thenReturn(List.of(sampleRun()));

        mockMvc.perform(get("/api/v1/reconciliation/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(1))
                .andExpect(jsonPath("$[0].source").value("BANK_A"));
    }

    @Test
    void getReconciliationRun_shouldReturn200WhenFound() throws Exception {
        when(reconciliationService.getReconciliationRun(1L))
                .thenReturn(sampleRun());

        mockMvc.perform(get("/api/v1/reconciliation/runs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getReconciliationResults_shouldReturn200() throws Exception {
        when(reconciliationService.getReconciliationResults(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reconciliation/runs/1/results"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
