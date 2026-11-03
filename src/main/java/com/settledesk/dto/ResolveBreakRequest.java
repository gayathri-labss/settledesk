
package com.settledesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveBreakRequest {

    @NotBlank(message = "Resolution notes are required")
    @Size(max = 2000, message = "Resolution notes cannot exceed 2000 characters")
    private String resolutionNotes;
}
