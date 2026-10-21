package com.akash.sentinel.provider;

import com.akash.sentinel.mcp.dto.LogSpanDto;

import java.util.List;

public interface LogProvider {
    List<LogSpanDto> queryByTraceId(String traceId);
    List<LogSpanDto> queryRecentErrors(int minutesBack);
}
