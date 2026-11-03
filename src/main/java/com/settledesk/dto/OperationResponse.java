package com.settledesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class OperationResponse {

    private Long operationId;
    private String operationType;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}