
package com.settledesk.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BreakSummaryResponse {

    private long totalBreaks;

    private long openCount;

    private long assignedCount;

    private long resolvedCount;

    private long mismatchCount;

    private long missingExternalCount;

    private long missingInternalCount;
}
