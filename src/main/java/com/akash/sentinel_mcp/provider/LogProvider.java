package com.akash.sentinel_mcp.provider;

import com.akash.sentinel_mcp.mcp.dto.LogSpanDto;

import java.util.List;

public interface LogProvider {
    List<LogSpanDto> queryByTraceId(String traceId);
    List<LogSpanDto> queryRecentErrors(int minutesBack);
}
