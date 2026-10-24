package com.akash.sentinel.service;

import com.akash.sentinel.mcp.dto.*;
import com.akash.sentinel.provider.LogProvider;
import com.akash.sentinel.rag.RunbookRagService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TriageOrchestratorService {

    private final LogProvider logProvider;
    private final PiiRedactionService piiRedactionService;
    private final TraceAnalyzerService traceAnalyzerService;
    private final RunbookRagService runbookRagService;

    public TriageOrchestratorService(LogProvider logProvider,
                                     PiiRedactionService piiRedactionService,
                                     TraceAnalyzerService traceAnalyzerService,
                                     RunbookRagService runbookRagService) {
        this.logProvider = logProvider;
        this.piiRedactionService = piiRedactionService;
        this.traceAnalyzerService = traceAnalyzerService;
        this.runbookRagService = runbookRagService;
    }

    public TriageReport orchestrateTriage(String traceId) {
        List<LogSpanDto> rawSpans = logProvider.queryByTraceId(traceId);

        List<LogSpanDto> sanitizedSpans = rawSpans.stream()
            .map(s -> new LogSpanDto(
                s.traceId(),
                s.spanId(),
                s.parentSpanId(),
                s.serviceName(),
                s.timestamp(),
                s.message() != null ? piiRedactionService.redact(s.message()) : null,
                s.exceptionClass(),
                s.level()
            ))
            .toList();

        LogSpanDto rootCause = traceAnalyzerService.isolateRootCause(sanitizedSpans);
        String normalizedQuery = rootCause != null
            ? traceAnalyzerService.normalizeErrorSignature(rootCause)
            : "unknown error";

        List<RunbookMatchDto> matchedRunbooks = runbookRagService.search(
            rootCause != null ? rootCause.serviceName() : null,
            rootCause != null ? rootCause.exceptionClass() : null,
            normalizedQuery
        );

        ErrorSpikeDto errorSpike = traceAnalyzerService.aggregateErrors(sanitizedSpans);

        return new TriageReport(
            traceId,
            rootCause,
            errorSpike,
            matchedRunbooks,
            "Triage complete. Matched " + matchedRunbooks.size() + " runbook(s) for root cause: " +
                (rootCause != null ? rootCause.serviceName() : "N/A")
        );
    }
}
