package com.akash.sentinel.mcp.dto;

public record RunbookMatchDto(
    String runbookId,
    String title,
    double vectorScore,
    double exactScore,
    double finalScore
) {}
