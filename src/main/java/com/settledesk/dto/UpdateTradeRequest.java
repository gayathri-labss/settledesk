package com.settledesk.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
public class UpdateTradeRequest {

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