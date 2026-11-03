package com.settledesk.dto;

import com.settledesk.model.BreakPriority;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBreakRequest {

    private BreakPriority priority;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
}