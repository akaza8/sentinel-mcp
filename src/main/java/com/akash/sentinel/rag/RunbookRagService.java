package com.akash.sentinel.rag;

import com.akash.sentinel.mcp.dto.RunbookMatchDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RunbookRagService {

    private final VectorStore vectorStore;
    private static final double SIMILARITY_THRESHOLD = 0.72;
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

            var builder = SearchRequest.builder()
                    .query(query != null ? query : "incident triage")
                    .topK(TOP_K)
                    .similarityThreshold(SIMILARITY_THRESHOLD);

            if (filterExpression != null) {
                builder.filterExpression(filterExpression);
            }

            SearchRequest request = builder.build();

            List<Document> candidates = vectorStore.similaritySearch(request);

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
            return List.of();
        }
    }

    private double extractVectorScore(Document doc) {
        if (doc.getMetadata() != null && doc.getMetadata().containsKey("similarity_score")) {
            Object val = doc.getMetadata().get("similarity_score");
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            }
        }
        return 0.5;
    }

    private record ScoredResult(Document doc, double vectorScore, double exactScore, double finalScore) {}
}
