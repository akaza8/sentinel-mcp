package com.akash.sentinel_mcp.provider;

import com.akash.sentinel_mcp.mcp.dto.LogSpanDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockChaosLogProviderTest {

    private final MockChaosLogProvider provider = new MockChaosLogProvider();

    @Test
    @DisplayName("Returns spans sorted in ascending chronological order for trace-db-901")
    void returnsSpansSortedByTimestamp() {
        List<LogSpanDto> spans = provider.queryByTraceId("trace-db-901");

        assertThat(spans).isNotEmpty();
        assertThat(spans).hasSize(3);
        
        for (int i = 0; i < spans.size() - 1; i++) {
            Instant current = spans.get(i).timestamp();
            Instant next = spans.get(i + 1).timestamp();
            assertThat(current).isBeforeOrEqualTo(next);
        }
    }

    @Test
    @DisplayName("Returns spans sorted in ascending chronological order for trace-stripe-504")
    void returnsStripeSpansSorted() {
        List<LogSpanDto> spans = provider.queryByTraceId("trace-stripe-504");

        assertThat(spans).isNotEmpty();
        assertThat(spans).hasSize(3);
        
        Instant first = spans.get(0).timestamp();
        Instant second = spans.get(1).timestamp();
        Instant third = spans.get(2).timestamp();
        
        assertThat(first).isBeforeOrEqualTo(second);
        assertThat(second).isBeforeOrEqualTo(third);
    }

    @Test
    @DisplayName("Returns spans sorted in ascending chronological order for trace-redis-772")
    void returnsRedisSpansSorted() {
        List<LogSpanDto> spans = provider.queryByTraceId("trace-redis-772");

        assertThat(spans).isNotEmpty();
        assertThat(spans).hasSize(3);
        
        for (int i = 0; i < spans.size() - 1; i++) {
            assertThat(spans.get(i).timestamp()).isBeforeOrEqualTo(spans.get(i + 1).timestamp());
        }
    }

    @Test
    @DisplayName("Returns empty list for non-existent trace ID")
    void returnsEmptyListForNonExistentTrace() {
        List<LogSpanDto> spans = provider.queryByTraceId("trace-does-not-exist");

        assertThat(spans).isNotNull();
        assertThat(spans).isEmpty();
    }

    @Test
    @DisplayName("Returns empty list for null trace ID")
    void returnsEmptyListForNullTraceId() {
        List<LogSpanDto> spans = provider.queryByTraceId(null);

        assertThat(spans).isNotNull();
        assertThat(spans).isEmpty();
    }

    @Test
    @DisplayName("Returns recent errors containing only ERROR level spans")
    void returnsOnlyErrorLevelSpans() {
        List<LogSpanDto> errors = provider.queryRecentErrors(60);

        assertThat(errors).isNotEmpty();
        assertThat(errors).allMatch(span -> "ERROR".equals(span.level()));
    }

    @Test
    @DisplayName("Returns recent errors with exception classes")
    void returnsErrorsWithExceptionClasses() {
        List<LogSpanDto> errors = provider.queryRecentErrors(60);

        assertThat(errors).isNotEmpty();
        assertThat(errors).allMatch(span -> span.exceptionClass() != null && !span.exceptionClass().isEmpty());
    }

    @Test
    @DisplayName("Returns expected number of recent errors")
    void returnsExpectedNumberOfErrors() {
        List<LogSpanDto> errors = provider.queryRecentErrors(60);

        assertThat(errors).hasSize(3);
    }

    @Test
    @DisplayName("Recent errors contain expected exception types")
    void recentErrorsContainExpectedExceptionTypes() {
        List<LogSpanDto> errors = provider.queryRecentErrors(60);

        assertThat(errors).extracting(LogSpanDto::exceptionClass)
            .containsExactlyInAnyOrder(
                "java.sql.SQLTransientConnectionException",
                "com.stripe.net.ApiConnectionException",
                "redis.clients.jedis.exceptions.JedisConnectionException"
            );
    }

    @Test
    @DisplayName("Recent errors contain expected services")
    void recentErrorsContainExpectedServices() {
        List<LogSpanDto> errors = provider.queryRecentErrors(60);

        assertThat(errors).extracting(LogSpanDto::serviceName)
            .containsExactlyInAnyOrder(
                "order-service",
                "payment-service",
                "cache-service"
            );
    }

    @Test
    @DisplayName("DB trace contains expected span structure")
    void dbTraceContainsExpectedStructure() {
        List<LogSpanDto> spans = provider.queryByTraceId("trace-db-901");

        assertThat(spans).hasSize(3);
        assertThat(spans.get(0).level()).isEqualTo("INFO");
        assertThat(spans.get(1).level()).isEqualTo("ERROR");
        assertThat(spans.get(1).exceptionClass()).isEqualTo("java.sql.SQLTransientConnectionException");
        assertThat(spans.get(2).level()).isEqualTo("WARN");
    }

    @Test
    @DisplayName("Stripe trace contains expected span structure")
    void stripeTraceContainsExpectedStructure() {
        List<LogSpanDto> spans = provider.queryByTraceId("trace-stripe-504");

        assertThat(spans).hasSize(3);
        assertThat(spans.get(0).level()).isEqualTo("INFO");
        assertThat(spans.get(1).level()).isEqualTo("ERROR");
        assertThat(spans.get(1).exceptionClass()).isEqualTo("com.stripe.net.ApiConnectionException");
        assertThat(spans.get(2).level()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Redis trace contains expected span structure")
    void redisTraceContainsExpectedStructure() {
        List<LogSpanDto> spans = provider.queryByTraceId("trace-redis-772");

        assertThat(spans).hasSize(3);
        assertThat(spans.get(0).level()).isEqualTo("INFO");
        assertThat(spans.get(1).level()).isEqualTo("ERROR");
        assertThat(spans.get(1).exceptionClass()).isEqualTo("redis.clients.jedis.exceptions.JedisConnectionException");
        assertThat(spans.get(2).level()).isEqualTo("WARN");
    }

    @Test
    @DisplayName("All traces have proper parent-child relationships")
    void tracesHaveProperParentChildRelationships() {
        List<LogSpanDto> dbSpans = provider.queryByTraceId("trace-db-901");
        
        assertThat(dbSpans.get(0).parentSpanId()).isNull();
        assertThat(dbSpans.get(1).parentSpanId()).isEqualTo("span-db-1");
        assertThat(dbSpans.get(2).parentSpanId()).isEqualTo("span-db-2");
    }
}
