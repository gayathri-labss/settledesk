package com.settledesk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class StartReconciliationRequest {

    @NotBlank
    private String source;

    @NotNull
    private LocalDate settlementDate;

    @NotEmpty
    @Valid
    private List<ExternalSettlementInput> externalRecords;
}
