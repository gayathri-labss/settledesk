
package com.settledesk.controller;

import com.settledesk.service.CounterpartyService;
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

@WebMvcTest(CounterpartyController.class)
class CounterpartyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CounterpartyService counterpartyService;

    @Test
    void searchCounterpartiesReturnsOk() throws Exception {
        when(counterpartyService.searchCounterparties(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/counterparties"))
                .andExpect(status().isOk());
    }

    @Test
    void searchCounterpartiesSupportsFilters() throws Exception {
        when(counterpartyService.searchCounterparties(
                eq("CP001"), eq("ABC"), eq("BANK"), eq("IN"), eq(true),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/counterparties")
                        .param("code", "CP001")
                        .param("name", "ABC")
                        .param("type", "BANK")
                        .param("country", "IN")
                        .param("active", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void searchCounterpartiesRejectsInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/counterparties")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(counterpartyService);
    }

    @Test
    void searchCounterpartiesRejectsInvalidDirection() throws Exception {
        mockMvc.perform(get("/api/v1/counterparties")
                        .param("direction", "sideways"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(counterpartyService);
    }

    @Test
    void searchCounterpartiesRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/counterparties")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/counterparties")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(counterpartyService);
    }

    @Test
    void getCounterpartyByIdReturnsOk() throws Exception {
        when(counterpartyService.getCounterpartyById(1L))
                .thenReturn(null);

        mockMvc.perform(get("/api/v1/counterparties/1"))
                .andExpect(status().isOk());
    }
}
