package com.akash.sentinel.rag;

import com.akash.sentinel.mcp.dto.RunbookMatchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RunbookRagService {

    private static final Logger log = LoggerFactory.getLogger(RunbookRagService.class);
    private final VectorStore vectorStore;
    private static final double SIMILARITY_THRESHOLD = 0.55;
    private static final int TOP_K = 4;

    public RunbookRagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<RunbookMatchDto> search(String targetService, String exceptionClass, String query) {
        try {
            String filterExpression = null;
            if (targetService != null && !targetService.isBlank()) {
                filterExpression = "service == '" + targetService + "'";
            }
            String searchQuery = (query != null && !query.isBlank())
                    ? query
                    : "incident triage";
            var builder = SearchRequest.builder()
                    .query(searchQuery)
                    .topK(TOP_K)
                    .similarityThreshold(SIMILARITY_THRESHOLD);

            if (filterExpression != null) {
                builder.filterExpression(filterExpression);
            }

            SearchRequest request = builder.build();

            List<Document> candidates = vectorStore.similaritySearch(request);
            if (candidates.isEmpty()
                    && targetService != null
                    && !targetService.isBlank()) {

                log.info(
                        "No runbooks found for service '{}'. Retrying without service filter.",
                        targetService
                );

                SearchRequest fallbackRequest = SearchRequest.builder()
                        .query(searchQuery)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build();

                candidates = vectorStore.similaritySearch(fallbackRequest);
            }

            List<ScoredResult> scored = candidates.stream()
                    .map(doc -> {
                        double vectorScore = extractVectorScore(doc);
                        String docException = doc.getMetadata() != null ? (String) doc.getMetadata().get("primary_exception") : null;
                        double exactScore = (exceptionClass != null && exceptionClass.equals(docException)) ? 1.0 : 0.0;
                        double finalScore = (vectorScore * 0.70) + (exactScore * 0.30);
                        return new ScoredResult(doc, vectorScore, exactScore, finalScore);

                    })
                    .sorted(Comparator.comparingDouble(ScoredResult::finalScore).reversed())
                    .limit(2)
                    .collect(Collectors.toList());

            return scored.stream()
                    .map(r -> new RunbookMatchDto(
                            (String) r.doc.getMetadata().get("runbook_id"),
                            (String) r.doc.getMetadata().get("title"),
                            r.vectorScore,
                            r.exactScore,
                            r.finalScore
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Runbook retrieval failed", e);
            throw new IllegalStateException("Runbook retrieval failed", e);
        }
    }

    private double extractVectorScore(Document doc) {
        if (doc.getMetadata() == null) {
            throw new IllegalStateException(
                    "Missing metadata in vector search result"
            );
        }

        Object distanceValue = doc.getMetadata().get("distance");

        if (distanceValue instanceof Number distance) {
            return Math.max(0.0, Math.min(1.0, 1.0 - distance.doubleValue()));
        }

        throw new IllegalStateException(
                "Vector search result has no numeric distance: "
                        + doc.getMetadata()
        );
    }

    private record ScoredResult(Document doc, double vectorScore, double exactScore, double finalScore) {}
}
