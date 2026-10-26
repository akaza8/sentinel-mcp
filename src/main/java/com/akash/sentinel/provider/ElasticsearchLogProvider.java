package com.akash.sentinel.provider;

import com.akash.sentinel.mcp.dto.LogSpanDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "sentinel.log-provider", havingValue = "elasticsearch")
public class ElasticsearchLogProvider implements LogProvider {

    private final RestClient restClient;

    public ElasticsearchLogProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:9200")
                .requestFactory(factory)
                .build();
    }

    @Override
    public List<LogSpanDto> queryByTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return List.of();
        }
        String query = "{\"query\":{\"term\":{\"trace_id\":\"" + traceId + "\"}}}";
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/_search")
                    .body(query)
                    .retrieve()
                    .body(Map.class);
            return parseEsResponse(response);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<LogSpanDto> queryRecentErrors(int minutesBack) {
        String query = "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"level\":\"error\"}}]}}}";
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/_search")
                    .body(query)
                    .retrieve()
                    .body(Map.class);
            return parseEsResponse(response);
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<LogSpanDto> parseEsResponse(Map<String, Object> response) {
        List<LogSpanDto> results = new ArrayList<>();
        if (response == null || !response.containsKey("hits")) {
            return results;
        }
        Map<String, Object> hits = (Map<String, Object>) response.get("hits");
        if (hits == null || !hits.containsKey("hits")) {
            return results;
        }
        List<Object> hitsList = (List<Object>) hits.get("hits");
        if (hitsList == null) {
            return results;
        }
        for (Object hitObj : hitsList) {
            if (hitObj instanceof Map) {
                Map<String, Object> hit = (Map<String, Object>) hitObj;
                if (hit.containsKey("_source")) {
                    Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                    String traceId = source.getOrDefault("trace_id", "es-trace").toString();
                    String service = source.getOrDefault("service", "es-service").toString();
                    String msg = source.getOrDefault("message", "").toString();
                    String exceptionClass = source.getOrDefault("exception_class", "").toString();
                    String tsStr = source.getOrDefault("@timestamp", Instant.now().toString()).toString();
                    results.add(new LogSpanDto(
                            traceId, "es-span", null, service,
                            Instant.parse(tsStr.replace("Z", "+00:00")), "ERROR", msg, exceptionClass.isEmpty() ? null : exceptionClass
                    ));
                }
            }
        }
        return results;
    }
}
