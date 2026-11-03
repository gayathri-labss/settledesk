package com.settledesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AuditEventResponse {

    private Long auditId;

    private String eventType;

    private Long entityId;

    private String action;

    private String details;

    private LocalDateTime performedAt;
}
