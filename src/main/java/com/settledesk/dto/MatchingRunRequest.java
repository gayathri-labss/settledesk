package com.settledesk.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchingRunRequest {

    @NotEmpty(message = "At least one trade ID is required")
    private List<@NotNull @Positive Long> tradeIds;
}