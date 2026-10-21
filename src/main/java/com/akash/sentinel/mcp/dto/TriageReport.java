package com.akash.sentinel.mcp.dto;

import java.util.List;

public record TriageReport(
    String traceId,
    LogSpanDto rootCauseSpan,
    ErrorSpikeDto errorSpike,
    List<RunbookMatchDto> matchedRunbooks,
    String recommendation
) {}
