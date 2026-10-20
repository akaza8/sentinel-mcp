package com.akash.sentinel_mcp.provider;

import com.akash.sentinel_mcp.mcp.dto.LogSpanDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "sentinel.log-provider", havingValue = "mock")
public class MockChaosLogProvider implements LogProvider {

    @Override
    public List<LogSpanDto> queryByTraceId(String traceId) {
        if (traceId == null) {
            return List.of();
        }
        return switch (traceId) {
            case "trace-db-901" -> dbTrace();
            case "trace-stripe-504" -> stripeTrace();
            case "trace-redis-772" -> redisTrace();
            default -> List.of();
        };
    }

    @Override
    public List<LogSpanDto> queryRecentErrors(int minutesBack) {
        return List.of(
            new LogSpanDto("trace-db-901", "span-db-1", null, "order-service", Instant.parse("2024-05-20T10:00:00Z"), "ERROR", "Connection pool exhausted: HikariPool-1 - Thread starvation", "java.sql.SQLTransientConnectionException"),
            new LogSpanDto("trace-stripe-504", "span-stripe-1", null, "payment-service", Instant.parse("2024-05-20T10:05:00Z"), "ERROR", "Stripe gateway timeout: 504 Gateway Timeout", "com.stripe.net.ApiConnectionException"),
            new LogSpanDto("trace-redis-772", "span-redis-1", null, "cache-service", Instant.parse("2024-05-20T10:10:00Z"), "ERROR", "Redis connection refused: Connection refused (Connection refused)", "redis.clients.jedis.exceptions.JedisConnectionException")
        );
    }

    private List<LogSpanDto> dbTrace() {
        return List.of(
            new LogSpanDto("trace-db-901", "span-db-1", null, "order-service", Instant.parse("2024-05-20T10:00:00Z"), "INFO", "Processing order 42", null),
            new LogSpanDto("trace-db-901", "span-db-2", "span-db-1", "db-service", Instant.parse("2024-05-20T10:00:01Z"), "ERROR", "Connection pool exhausted: HikariPool-1 - Thread starvation", "java.sql.SQLTransientConnectionException"),
            new LogSpanDto("trace-db-901", "span-db-3", "span-db-2", "db-service", Instant.parse("2024-05-20T10:00:02Z"), "WARN", "Retry attempt 1 failed", null)
        );
    }

    private List<LogSpanDto> stripeTrace() {
        return List.of(
            new LogSpanDto("trace-stripe-504", "span-stripe-1", null, "payment-service", Instant.parse("2024-05-20T10:05:00Z"), "INFO", "Initiating charge for customer 99", null),
            new LogSpanDto("trace-stripe-504", "span-stripe-2", "span-stripe-1", "stripe-gateway", Instant.parse("2024-05-20T10:05:02Z"), "ERROR", "Stripe gateway timeout: 504 Gateway Timeout", "com.stripe.net.ApiConnectionException"),
            new LogSpanDto("trace-stripe-504", "span-stripe-3", "span-stripe-2", "payment-service", Instant.parse("2024-05-20T10:05:03Z"), "ERROR", "Payment failed: upstream timeout", "com.stripe.net.ApiConnectionException")
        );
    }

    private List<LogSpanDto> redisTrace() {
        return List.of(
            new LogSpanDto("trace-redis-772", "span-redis-1", null, "cache-service", Instant.parse("2024-05-20T10:10:00Z"), "INFO", "Fetching session token", null),
            new LogSpanDto("trace-redis-772", "span-redis-2", "span-redis-1", "redis-cluster", Instant.parse("2024-05-20T10:10:01Z"), "ERROR", "Redis connection refused: Connection refused (Connection refused)", "redis.clients.jedis.exceptions.JedisConnectionException"),
            new LogSpanDto("trace-redis-772", "span-redis-3", "span-redis-2", "cache-service", Instant.parse("2024-05-20T10:10:02Z"), "WARN", "Falling back to DB lookup", null)
        );
    }
}
