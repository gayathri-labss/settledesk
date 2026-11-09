
package com.settledesk.controller;

import com.settledesk.model.MatchingResult;
import com.settledesk.model.MatchingRun;
import com.settledesk.service.MatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchingController.class)
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchingService matchingService;

    @Test
    void startMatchingRunReturnsCreated() throws Exception {
        when(matchingService.startMatchingRun(any()))
                .thenReturn(mock(MatchingRun.class));

        mockMvc.perform(post("/api/v1/matching/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tradeIds":[1,2]}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void startMatchingRunRejectsEmptyTradeIds() throws Exception {
        mockMvc.perform(post("/api/v1/matching/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tradeIds":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startMatchingRunRejectsInvalidTradeIds() throws Exception {
        mockMvc.perform(post("/api/v1/matching/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tradeIds":[0,-1]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllMatchingRunsReturnsOk() throws Exception {
        when(matchingService.getAllMatchingRuns()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/matching/runs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getMatchingRunByIdReturnsOk() throws Exception {
        when(matchingService.getMatchingRunById(1L))
                .thenReturn(mock(MatchingRun.class));

        mockMvc.perform(get("/api/v1/matching/runs/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getMatchingResultsReturnsOk() throws Exception {
        when(matchingService.getMatchingResultsByRunId(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/matching/runs/1/results"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getMatchingExceptionsReturnsOk() throws Exception {
        when(matchingService.getMatchingExceptions())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/matching/exceptions"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void retryMatchingRunReturnsCreated() throws Exception {
        when(matchingService.retryMatchingRun(1L))
                .thenReturn(mock(MatchingRun.class));

        mockMvc.perform(post("/api/v1/matching/runs/1/retry"))
                .andExpect(status().isCreated());
    }
}
