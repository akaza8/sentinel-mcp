package com.akash.sentinel_mcp.mcp.dto;

public record RunbookMatchDto(
    String runbookId,
    String title,
    double vectorScore,
    double exactScore,
    double finalScore
) {}
