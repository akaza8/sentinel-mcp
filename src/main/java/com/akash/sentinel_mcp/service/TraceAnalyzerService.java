package com.akash.sentinel_mcp.service;

import com.akash.sentinel_mcp.mcp.dto.LogSpanDto;
import com.akash.sentinel_mcp.mcp.dto.ErrorSpikeDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TraceAnalyzerService {

    public LogSpanDto isolateRootCause(List<LogSpanDto> spans) {
        if (spans == null || spans.isEmpty()) return null;
        return spans.stream()
            .filter(s -> s.exceptionClass() != null && !s.exceptionClass().isEmpty())
            .min(Comparator.comparing(LogSpanDto::timestamp))
            .orElse(spans.get(0));
    }

    public String normalizeErrorSignature(LogSpanDto span) {
        if (span == null || span.exceptionClass() == null) return "unknown";
        String normalizedMessage = span.message().replaceAll("\\d+", "n").toLowerCase();
        return span.exceptionClass() + ":" + normalizedMessage;
    }

    public ErrorSpikeDto aggregateErrors(List<LogSpanDto> spans) {
        Map<String, List<LogSpanDto>> grouped = spans.stream()
            .filter(s -> s.exceptionClass() != null && !s.exceptionClass().isEmpty())
            .collect(Collectors.groupingBy(LogSpanDto::exceptionClass));

        String topClass = grouped.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
            .map(Map.Entry::getKey).orElse("None");

        List<LogSpanDto> topSpans = grouped.getOrDefault(topClass, List.of());
        return new ErrorSpikeDto(
            topClass,
            topSpans.size(),
            topSpans.stream().map(LogSpanDto::timestamp).min(Comparator.naturalOrder()).orElse(null),
            topSpans.stream().map(LogSpanDto::timestamp).max(Comparator.naturalOrder()).orElse(null),
            normalizeErrorSignature(topSpans.isEmpty() ? null : topSpans.get(0)),
            topSpans.stream().map(LogSpanDto::serviceName).distinct().toList()
        );
    }
}
