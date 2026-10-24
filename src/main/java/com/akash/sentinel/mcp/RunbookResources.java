package com.akash.sentinel.mcp;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class RunbookResources {

    @McpResource(uri = "runbook://{runbookId}")
    public String getRunbook(String runbookId) throws Exception {
        Path path = Paths.get("runbooks", runbookId + ".md");
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        return "Runbook " + runbookId + " content not available locally.";
    }
}
