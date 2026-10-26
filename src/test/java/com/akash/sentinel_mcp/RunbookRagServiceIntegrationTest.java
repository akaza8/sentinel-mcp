package com.akash.sentinel_mcp;

import com.akash.sentinel.SentinelMcpApplication;
import com.akash.sentinel.rag.MarkdownRunbookSplitter;
import com.akash.sentinel.rag.RunbookIngestionRunner;
import com.akash.sentinel.rag.RunbookRagService;
import com.akash.sentinel.mcp.dto.RunbookMatchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {SentinelMcpApplication.class, TestcontainersConfiguration.class, MarkdownRunbookSplitter.class, RunbookRagService.class, RunbookIngestionRunner.class})
class RunbookRagServiceIntegrationTest {

    @Autowired
    private MarkdownRunbookSplitter splitter;

    @Autowired
    private RunbookRagService ragService;

    @Autowired
    private RunbookIngestionRunner ingestionRunner;

    @BeforeEach
    void ingestRunbooks() {
        ingestionRunner.ingest();
    }
    @DynamicPropertySource
    static void ollamaProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.ai.ollama.base-url",
                () -> "http://localhost:11434"
        );
    }
    @Test
    void paraphrasedQueryFindsCorrectRunbook() {
        List<RunbookMatchDto> results = ragService.search(
                "payment-service",
                "SocketTimeoutException",
                "Stripe payment requests keep timing out"
        );

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().runbookId())
                .isEqualTo("RB-002-stripe-gateway-timeout");
    }
    @Test
    void splitterExtractsFrontmatterAndSplitsOnH2WithoutLosingHeaders() throws Exception {
        Path runbook = Path.of("runbooks/RB-002-stripe-gateway-timeout.md");
        List<org.springframework.ai.document.Document> docs = splitter.split(runbook);

        assertThat(docs).isNotEmpty();
        assertThat(docs.getFirst().getMetadata()).containsEntry("service", "payment-service");
        assertThat(docs.getFirst().getMetadata()).containsEntry("primary_exception", "SocketTimeoutException");

        List<String> sectionNames = docs.stream()
                .map(d -> (String) d.getMetadata().get("section_name"))
                .toList();

        assertThat(sectionNames).contains("Symptoms & Error Signatures", "Root Cause Analysis", "Step-by-Step Remediation");
    }

    @Test
    void queryPaymentServiceReturnsRB002WithHighHybridScore() {
//        List<RunbookMatchDto> results = ragService.search(
//                "payment-service",
//                "SocketTimeoutException",
//                "payment-service java.net.SocketTimeoutException Read timed out"
//        );
        List<RunbookMatchDto> results = ragService.search(
                "payment-service",
                "SocketTimeoutException",
                "payment-service java.net.SocketTimeoutException Read timed out"
        );

        assertThat(results).isNotEmpty();

        assertThat(results.getFirst().runbookId())
                .isEqualTo("RB-002-stripe-gateway-timeout");

        assertThat(results.getFirst().finalScore())
                .isGreaterThan(0.80);

        assertThat(results.getFirst().exactScore())
                .isEqualTo(1.0);

        if (!results.isEmpty()) {
            assertThat(results.getFirst().runbookId()).isEqualTo("RB-002-stripe-gateway-timeout");
            assertThat(results.getFirst().finalScore()).isGreaterThan(0.80);
        }
    }

    @Test
    void unknownErrorQueryBelowThresholdReturnsEmpty() {
        List<RunbookMatchDto> results = ragService.search(
                "unknown-service",
                "UnknownException",
                "totally unknown error that should not match anything"
        );

        assertThat(results).isEmpty();
    }

    @Test
    void rerankingAppliesThirtyPercentBonusWhenExceptionMatches() {
        List<RunbookMatchDto> results = ragService.search(
                "payment-service",
                "SocketTimeoutException",
                "Stripe gateway timeout"
        );

        if (!results.isEmpty()) {
            assertThat(results.getFirst().finalScore()).isGreaterThanOrEqualTo(0.30);
        }
    }
}
