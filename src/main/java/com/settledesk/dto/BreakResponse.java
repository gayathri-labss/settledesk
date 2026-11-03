package com.settledesk.dto;

import com.settledesk.model.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakResponse {

    private Long breakId;
    private Long resultId;
    private Long runId;
    private String settlementReference;
    private BreakCategory category;
    private BreakStatus status;
    private BreakPriority priority;
    private String description;
    private String assignedTo;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}