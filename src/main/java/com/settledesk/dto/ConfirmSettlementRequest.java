package com.settledesk.dto;

import com.settledesk.model.SettlementOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmSettlementRequest {

    @NotNull(message = "Settlement outcome is required")
    private SettlementOutcome outcome;

    @Size(max = 1000, message = "Failure reason cannot exceed 1000 characters")
    private String failureReason;
}