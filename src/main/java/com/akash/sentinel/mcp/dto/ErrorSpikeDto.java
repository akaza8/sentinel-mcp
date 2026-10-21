package com.akash.sentinel.mcp.dto;

import java.time.Instant;
import java.util.List;

public record ErrorSpikeDto(
    String exceptionClass,
    int count,
    Instant firstSeen,
    Instant lastSeen,
    String normalizedSignature,
    List<String> affectedServices
) {}
