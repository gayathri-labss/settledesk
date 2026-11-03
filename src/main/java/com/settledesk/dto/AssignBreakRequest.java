
package com.settledesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignBreakRequest {

    @NotBlank(message = "Assignee is required")
    @Size(max = 255, message = "Assignee cannot exceed 255 characters")
    private String assignedTo;
}
