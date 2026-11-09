
package com.settledesk.controller;

import com.settledesk.dto.*;
import com.settledesk.service.BreakService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BreakController.class)
class BreakControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BreakService breakService;

    private BreakResponse sampleBreak() {
        return BreakResponse.builder()
                .breakId(1L)
                .description("Settlement mismatch")
                .build();
    }

    @Test
    void searchBreaksReturnsOk() throws Exception {
        when(breakService.searchBreaks(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/breaks"))
                .andExpect(status().isOk());
    }

    @Test
    void searchBreaksRejectsNegativePage() {
        assertThrows(ServletException.class,
                () -> mockMvc.perform(get("/api/v1/breaks")
                        .param("page", "-1")));
    }

    @Test
    void searchBreaksRejectsInvalidPageSize() {
        assertThrows(ServletException.class,
                () -> mockMvc.perform(get("/api/v1/breaks")
                        .param("size", "101")));
    }

    @Test
    void getBreakByIdReturnsOk() throws Exception {
        when(breakService.getBreakById(1L)).thenReturn(sampleBreak());

        mockMvc.perform(get("/api/v1/breaks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakId").value(1));
    }

    @Test
    void updateBreakReturnsOk() throws Exception {
        when(breakService.updateBreak(eq(1L), any()))
                .thenReturn(sampleBreak());

        mockMvc.perform(patch("/api/v1/breaks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priority": "HIGH",
                                  "description": "Updated mismatch"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void updateBreakRejectsOversizedDescription() throws Exception {
        String description = "x".repeat(2001);

        mockMvc.perform(patch("/api/v1/breaks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"%s"}
                                """.formatted(description)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolveBreakReturnsOk() throws Exception {
        when(breakService.resolveBreak(eq(1L), any()))
                .thenReturn(sampleBreak());

        mockMvc.perform(post("/api/v1/breaks/1/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resolutionNotes":"Resolved after verification"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void resolveBreakRejectsBlankResolutionNotes() throws Exception {
        mockMvc.perform(post("/api/v1/breaks/1/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resolutionNotes":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignBreakReturnsOk() throws Exception {
        when(breakService.assignBreak(eq(1L), any()))
                .thenReturn(sampleBreak());

        mockMvc.perform(post("/api/v1/breaks/1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedTo":"operations-team"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void assignBreakRejectsBlankAssignee() throws Exception {
        mockMvc.perform(post("/api/v1/breaks/1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedTo":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBreakSummaryReturnsOk() throws Exception {
        when(breakService.getBreakSummary())
                .thenReturn(new BreakSummaryResponse());

        mockMvc.perform(get("/api/v1/breaks/summary"))
                .andExpect(status().isOk());
    }
}
