package com.settledesk.dto;

import com.settledesk.model.TradeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTradeStatusRequest {

    @NotNull
    private TradeStatus status;
}