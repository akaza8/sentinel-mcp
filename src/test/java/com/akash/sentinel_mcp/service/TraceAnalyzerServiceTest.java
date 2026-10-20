package com.akash.sentinel_mcp.service;

import com.akash.sentinel_mcp.mcp.dto.LogSpanDto;
import com.akash.sentinel_mcp.mcp.dto.ErrorSpikeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TraceAnalyzerServiceTest {

    private final TraceAnalyzerService service = new TraceAnalyzerService();

    @Test
    @DisplayName("Isolates earliest chronological ERROR span as root cause")
    void isolatesEarliestErrorAsRootCause() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        Instant t2 = Instant.parse("2024-05-20T10:00:01Z");
        Instant t3 = Instant.parse("2024-05-20T10:00:02Z");
        Instant t4 = Instant.parse("2024-05-20T10:00:03Z");

        LogSpanDto infoSpan = new LogSpanDto("trace-1", "span-1", null, "service-a", t1, "INFO", "Processing request", null);
        LogSpanDto firstError = new LogSpanDto("trace-1", "span-2", "span-1", "service-b", t2, "ERROR", "DB Connection Failed", "java.sql.SQLException");
        LogSpanDto warnSpan = new LogSpanDto("trace-1", "span-3", "span-2", "service-b", t3, "WARN", "Retry attempt 1", null);
        LogSpanDto cascadeError = new LogSpanDto("trace-1", "span-4", "span-2", "service-c", t4, "ERROR", "Timeout waiting for DB", "java.util.concurrent.TimeoutException");

        List<LogSpanDto> spans = List.of(infoSpan, firstError, warnSpan, cascadeError);
        LogSpanDto rootCause = service.isolateRootCause(spans);

        assertThat(rootCause).isNotNull();
        assertThat(rootCause.spanId()).isEqualTo("span-2");
        assertThat(rootCause.exceptionClass()).isEqualTo("java.sql.SQLException");
        assertThat(rootCause.serviceName()).isEqualTo("service-b");
    }

    @Test
    @DisplayName("Handles out-of-order spans and still finds earliest error")
    void handlesOutOfOrderSpans() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        Instant t2 = Instant.parse("2024-05-20T10:00:01Z");
        Instant t3 = Instant.parse("2024-05-20T10:00:02Z");

        LogSpanDto laterError = new LogSpanDto("trace-1", "span-3", "span-2", "service-c", t3, "ERROR", "Timeout", "java.util.concurrent.TimeoutException");
        LogSpanDto earlierError = new LogSpanDto("trace-1", "span-2", "span-1", "service-b", t2, "ERROR", "DB Connection Failed", "java.sql.SQLException");
        LogSpanDto infoSpan = new LogSpanDto("trace-1", "span-1", null, "service-a", t1, "INFO", "Start", null);

        List<LogSpanDto> spans = List.of(laterError, earlierError, infoSpan);
        LogSpanDto rootCause = service.isolateRootCause(spans);

        assertThat(rootCause).isNotNull();
        assertThat(rootCause.spanId()).isEqualTo("span-2");
        assertThat(rootCause.timestamp()).isEqualTo(t2);
    }

    @Test
    @DisplayName("Normalizes error signature by replacing digits and lowercasing")
    void normalizesErrorSignature() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        LogSpanDto span = new LogSpanDto(
            "trace-1", 
            "span-1", 
            null, 
            "order-service", 
            t1, 
            "ERROR", 
            "Connection pool exhausted: HikariPool-1 - Thread starvation", 
            "java.sql.SQLTransientConnectionException"
        );
        
        String signature = service.normalizeErrorSignature(span);
        
        assertThat(signature).isEqualTo("java.sql.SQLTransientConnectionException:connection pool exhausted: hikaripool-n - thread starvation");
        assertThat(signature).doesNotContain("1");
    }

    @Test
    @DisplayName("Normalizes signature with multiple numeric values")
    void normalizesMultipleNumbers() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        LogSpanDto span = new LogSpanDto(
            "trace-1", 
            "span-1", 
            null, 
            "api-service", 
            t1, 
            "ERROR", 
            "Error at line 42 in file handler_123.java thread-456", 
            "java.lang.RuntimeException"
        );
        
        String signature = service.normalizeErrorSignature(span);
        
        assertThat(signature).isEqualTo("java.lang.RuntimeException:error at line n in file handler_n.java thread-n");
        assertThat(signature).doesNotContain("42");
        assertThat(signature).doesNotContain("123");
        assertThat(signature).doesNotContain("456");
    }

    @Test
    @DisplayName("Handles timeline with only INFO spans (no failure)")
    void handlesNoErrors() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        Instant t2 = Instant.parse("2024-05-20T10:00:01Z");
        
        LogSpanDto infoSpan1 = new LogSpanDto("trace-1", "span-1", null, "service-a", t1, "INFO", "Start", null);
        LogSpanDto infoSpan2 = new LogSpanDto("trace-1", "span-2", "span-1", "service-b", t2, "INFO", "Processing", null);
        
        LogSpanDto rootCause = service.isolateRootCause(List.of(infoSpan1, infoSpan2));
        
        assertThat(rootCause).isNotNull();
        assertThat(rootCause.spanId()).isEqualTo("span-1");
        assertThat(rootCause.exceptionClass()).isNull();
    }

    @Test
    @DisplayName("Handles empty list")
    void handlesEmptyList() {
        LogSpanDto rootCause = service.isolateRootCause(List.of());
        
        assertThat(rootCause).isNull();
    }

    @Test
    @DisplayName("Handles null list")
    void handlesNullList() {
        LogSpanDto rootCause = service.isolateRootCause(null);
        
        assertThat(rootCause).isNull();
    }

    @Test
    @DisplayName("Normalizes signature with null span returns 'unknown'")
    void handlesNullSpanInNormalization() {
        String signature = service.normalizeErrorSignature(null);
        
        assertThat(signature).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Normalizes signature with null exception class returns 'unknown'")
    void handlesNullExceptionClass() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        LogSpanDto span = new LogSpanDto("trace-1", "span-1", null, "service-a", t1, "ERROR", "Some error", null);
        
        String signature = service.normalizeErrorSignature(span);
        
        assertThat(signature).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Aggregates errors and identifies top exception class")
    void aggregatesErrorsCorrectly() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        Instant t2 = Instant.parse("2024-05-20T10:00:01Z");
        Instant t3 = Instant.parse("2024-05-20T10:00:02Z");
        Instant t4 = Instant.parse("2024-05-20T10:00:03Z");

        LogSpanDto error1 = new LogSpanDto("trace-1", "span-1", null, "service-a", t1, "ERROR", "DB timeout", "java.sql.SQLException");
        LogSpanDto error2 = new LogSpanDto("trace-2", "span-2", null, "service-b", t2, "ERROR", "Connection failed", "java.sql.SQLException");
        LogSpanDto error3 = new LogSpanDto("trace-3", "span-3", null, "service-c", t3, "ERROR", "Timeout", "java.util.concurrent.TimeoutException");
        LogSpanDto error4 = new LogSpanDto("trace-4", "span-4", null, "service-a", t4, "ERROR", "Pool exhausted", "java.sql.SQLException");

        List<LogSpanDto> spans = List.of(error1, error2, error3, error4);
        ErrorSpikeDto spike = service.aggregateErrors(spans);

        assertThat(spike).isNotNull();
        assertThat(spike.exceptionClass()).isEqualTo("java.sql.SQLException");
        assertThat(spike.count()).isEqualTo(3);
        assertThat(spike.firstSeen()).isEqualTo(t1);
        assertThat(spike.lastSeen()).isEqualTo(t4);
        assertThat(spike.affectedServices()).containsExactlyInAnyOrder("service-a", "service-b");
    }

    @Test
    @DisplayName("Aggregates errors across multiple services")
    void tracksAffectedServices() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        Instant t2 = Instant.parse("2024-05-20T10:00:01Z");
        Instant t3 = Instant.parse("2024-05-20T10:00:02Z");

        LogSpanDto error1 = new LogSpanDto("trace-1", "span-1", null, "order-service", t1, "ERROR", "DB error", "java.sql.SQLException");
        LogSpanDto error2 = new LogSpanDto("trace-2", "span-2", null, "payment-service", t2, "ERROR", "DB error", "java.sql.SQLException");
        LogSpanDto error3 = new LogSpanDto("trace-3", "span-3", null, "inventory-service", t3, "ERROR", "DB error", "java.sql.SQLException");

        List<LogSpanDto> spans = List.of(error1, error2, error3);
        ErrorSpikeDto spike = service.aggregateErrors(spans);

        assertThat(spike.affectedServices()).hasSize(3);
        assertThat(spike.affectedServices()).containsExactlyInAnyOrder("order-service", "payment-service", "inventory-service");
    }

    @Test
    @DisplayName("Handles aggregation with no errors")
    void handlesAggregationWithNoErrors() {
        Instant t1 = Instant.parse("2024-05-20T10:00:00Z");
        LogSpanDto infoSpan = new LogSpanDto("trace-1", "span-1", null, "service-a", t1, "INFO", "Processing", null);

        List<LogSpanDto> spans = List.of(infoSpan);
        ErrorSpikeDto spike = service.aggregateErrors(spans);

        assertThat(spike).isNotNull();
        assertThat(spike.exceptionClass()).isEqualTo("None");
        assertThat(spike.count()).isZero();
        assertThat(spike.affectedServices()).isEmpty();
    }
}
