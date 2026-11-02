package com.settledesk.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTradeConfirmationRequest {

    @NotBlank
    private String counterpartyReference;

    @NotBlank
    private String instrument;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotBlank
    private String buyer;

    @NotBlank
    private String seller;
}