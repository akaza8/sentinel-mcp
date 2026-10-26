package com.akash.sentinel.rag;

import com.akash.sentinel.mcp.dto.RunbookMatchDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("Runbook RAG Integration")
class RunbookRagIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pgvector = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("sentinel_rag")
            .withUsername("postgres")
            .withPassword("secret");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", pgvector::getJdbcUrl);
        registry.add("spring.datasource.username", pgvector::getUsername);
        registry.add("spring.datasource.password", pgvector::getPassword);
    }

    @Autowired
    private RunbookRagService runbookRagService;

    @Test
    @DisplayName("Startup ingestion indexes markdown runbooks and hybrid search reranks")
    void hybridSearchReturnsScoredMatches() {
        List<RunbookMatchDto> results = runbookRagService.search(
                "db-service", "ConnectionPoolException", "database pool exhausted"
        );
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).runbookId()).isNotBlank();
        assertThat(results.get(0).finalScore()).isGreaterThanOrEqualTo(0.0);
    }
}
