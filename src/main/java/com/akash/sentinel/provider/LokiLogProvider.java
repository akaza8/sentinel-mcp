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
@ConditionalOnProperty(name = "sentinel.log-provider", havingValue = "loki")
public class LokiLogProvider implements LogProvider {

    private final RestClient restClient;

    public LokiLogProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:3100")
                .requestFactory(factory)
                .build();
    }

    @Override
    public List<LogSpanDto> queryByTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return List.of();
        }
        String query = "{trace_id=\"" + traceId + "\"}";
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/loki/api/v1/query_range?query=" + query + "&limit=100")
                    .retrieve()
                    .body(Map.class);
            return parseLokiResponse(response);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<LogSpanDto> queryRecentErrors(int minutesBack) {
        String query = "{level=\"error\"}";
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/loki/api/v1/query_range?query=" + query + "&limit=50")
                    .retrieve()
                    .body(Map.class);
            return parseLokiResponse(response);
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<LogSpanDto> parseLokiResponse(Map<String, Object> response) {
        List<LogSpanDto> results = new ArrayList<>();
        if (response == null || !response.containsKey("data")) {
            return results;
        }
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null || !data.containsKey("result")) {
            return results;
        }
        List<Object> result = (List<Object>) data.get("result");
        if (result == null) {
            return results;
        }
        for (Object item : result) {
            if (item instanceof Map) {
                Map<String, Object> stream = (Map<String, Object>) item;
                if (stream.containsKey("stream") && stream.containsKey("values")) {
                    List<List<String>> values = (List<List<String>>) stream.get("values");
                    if (values != null) {
                        for (List<String> pair : values) {
                            if (pair.size() >= 2) {
                                String ts = pair.get(0);
                                String msg = pair.get(1);
                                results.add(new LogSpanDto(
                                        "loki-trace", "loki-span", null, "loki-service",
                                        Instant.parse(ts.replace("Z", "+00:00")), "ERROR", msg, null
                                ));
                            }
                        }
                    }
                }
            }
        }
        return results;
    }
}
