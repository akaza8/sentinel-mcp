package com.akash.sentinel.mcp;

import com.akash.sentinel.mcp.dto.LogSpanDto;
import com.akash.sentinel.mcp.dto.RunbookMatchDto;
import com.akash.sentinel.mcp.dto.TriageReport;
import com.akash.sentinel.provider.LogProvider;
import com.akash.sentinel.rag.RunbookRagService;
import com.akash.sentinel.service.TriageOrchestratorService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentTriageTools {

    private final TriageOrchestratorService triageOrchestratorService;
    private final LogProvider logProvider;
    private final RunbookRagService runbookRagService;

    public IncidentTriageTools(TriageOrchestratorService triageOrchestratorService,
                               LogProvider logProvider,
                               RunbookRagService runbookRagService) {
        this.triageOrchestratorService = triageOrchestratorService;
        this.logProvider = logProvider;
        this.runbookRagService = runbookRagService;
    }

    @McpTool
    public TriageReport investigate_trace(
            @McpToolParam(description = "Distributed trace ID to analyze") String traceId) {
        return triageOrchestratorService.orchestrateTriage(traceId);
    }

    @McpTool
    public List<LogSpanDto> get_recent_service_errors(
            @McpToolParam(description = "Microservice name") String serviceName,
            @McpToolParam(description = "Lookback window in minutes") int lookbackMinutes) {
        List<LogSpanDto> errors = logProvider.queryRecentErrors(lookbackMinutes);
        if (serviceName != null && !serviceName.isBlank()) {
            return errors.stream()
                .filter(s -> serviceName.equals(s.serviceName()))
                .toList();
        }
        return errors;
    }

    @McpTool
    public List<RunbookMatchDto> search_runbooks(
            @McpToolParam(description = "Error message or technical query") String query,
            @McpToolParam(description = "Target service name", required = false) String serviceName) {
        return runbookRagService.search(serviceName, null, query);
    }
}
