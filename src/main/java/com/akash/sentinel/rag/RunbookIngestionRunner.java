package com.akash.sentinel.rag;

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
            return;
        }

        List<Document> allChunks = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".md"))
                  .forEach(p -> {
                      try {
                          allChunks.addAll(splitter.split(p));
                      } catch (IOException e) {
                          // Log and continue; do not block other runbooks
                      }
                  });
        } catch (IOException e) {
            // Log directory read failure
        }

        if (!allChunks.isEmpty()) {
            try {
                vectorStore.add(allChunks);
            } catch (Exception e) {
                // Log vector store persistence error
            }
        }
    }
}
