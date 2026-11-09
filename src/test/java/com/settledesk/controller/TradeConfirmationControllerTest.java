
package com.settledesk.controller;

import com.settledesk.dto.CreateTradeConfirmationRequest;
import com.settledesk.model.TradeConfirmation;
import com.settledesk.service.TradeConfirmationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeConfirmationController.class)
class TradeConfirmationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeConfirmationService tradeConfirmationService;

    @Test
    void createConfirmationReturnsCreated() throws Exception {
        when(tradeConfirmationService.createConfirmation(eq(1L), any()))
                .thenReturn(mock(TradeConfirmation.class));

        mockMvc.perform(post("/api/v1/trades/1/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "counterpartyReference": "CP-001",
                                  "instrument": "NIFTY",
                                  "quantity": 10,
                                  "price": 250.50,
                                  "buyer": "Buyer A",
                                  "seller": "Seller B"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void createConfirmationRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/trades/1/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "counterpartyReference": "",
                                  "instrument": "",
                                  "quantity": 0,
                                  "price": -1,
                                  "buyer": "",
                                  "seller": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tradeConfirmationService);
    }

    @Test
    void getConfirmationReturnsOk() throws Exception {
        when(tradeConfirmationService.getConfirmationByTradeId(1L))
                .thenReturn(mock(TradeConfirmation.class));

        mockMvc.perform(get("/api/v1/trades/1/confirmations"))
                .andExpect(status().isOk());
    }
}
