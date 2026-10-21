package com.akash.sentinel.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class RunbookIngestionRunner {

    private static final Logger log = LoggerFactory.getLogger(RunbookIngestionRunner.class);

    private final MarkdownRunbookSplitter splitter;
    private final VectorStore vectorStore;

    public RunbookIngestionRunner(MarkdownRunbookSplitter splitter, VectorStore vectorStore) {
        this.splitter = splitter;
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingest() {
        Path dir = Path.of("runbooks");
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            log.warn("Runbooks directory not found: {}", dir.toAbsolutePath());
            return;
        }

        List<Document> allChunks = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".md"))
                  .forEach(p -> {
                      try {
                          allChunks.addAll(splitter.split(p));
                      } catch (IOException e) {
                          log.error("Failed to split runbook: {}", p, e);
                      }
                  });
        } catch (IOException e) {
            log.error("Failed to list runbooks directory: {}", dir, e);
        }

        if (!allChunks.isEmpty()) {
            try {
                vectorStore.add(allChunks);
                log.info("Indexed {} chunks into VectorStore", allChunks.size());
            } catch (Exception e) {
                log.error("Failed to persist chunks to VectorStore", e);
            }
        } else {
            log.info("No runbook chunks to index");
        }
    }
}
