package com.akash.sentinel_mcp.mcp.dto;

import java.time.Instant;

public record LogSpanDto(
    String traceId,
    String spanId,
    String parentSpanId,
    String serviceName,
    Instant timestamp,
    String level,
    String message,
    String exceptionClass
) {}
