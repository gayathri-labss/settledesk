
package com.settledesk.controller;

import com.settledesk.service.InstrumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstrumentController.class)
class InstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstrumentService instrumentService;

    @Test
    void searchInstrumentsReturnsOk() throws Exception {
        when(instrumentService.searchInstruments(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/instruments"))
                .andExpect(status().isOk());
    }

    @Test
    void searchInstrumentsSupportsFilters() throws Exception {
        when(instrumentService.searchInstruments(
                eq("NIFTY"), eq("Index"), eq("INDEX"), eq("INR"), eq(true),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/instruments")
                        .param("code", "NIFTY")
                        .param("name", "Index")
                        .param("type", "INDEX")
                        .param("currency", "INR")
                        .param("active", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void searchInstrumentsRejectsInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/instruments")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(instrumentService);
    }

    @Test
    void searchInstrumentsRejectsInvalidDirection() throws Exception {
        mockMvc.perform(get("/api/v1/instruments")
                        .param("direction", "sideways"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(instrumentService);
    }

    @Test
    void searchInstrumentsRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/instruments")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/instruments")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(instrumentService);
    }

    @Test
    void getInstrumentByIdReturnsOk() throws Exception {
        when(instrumentService.getInstrumentById(1L))
                .thenReturn(null);

        mockMvc.perform(get("/api/v1/instruments/1"))
                .andExpect(status().isOk());
    }
}
