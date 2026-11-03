package com.settledesk.dto;

import com.settledesk.model.ExternalSettlementStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateExternalSettlementRequest {

    @NotBlank(message = "External reference is required")
    @Size(max = 255)
    private String externalReference;

    @NotBlank(message = "Settlement reference is required")
    @Size(max = 255)
    private String settlementReference;

    @NotBlank(message = "Source is required")
    @Size(max = 255)
    private String source;

    @NotBlank(message = "Instrument is required")
    @Size(max = 255)
    private String instrument;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Settlement date is required")
    private LocalDate settlementDate;

    @NotNull(message = "External settlement status is required")
    private ExternalSettlementStatus status;
}