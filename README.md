ontributing to Sentinel-MCP

Thank you for your interest in contributing to Sentinel-MCP! Whether you are adding a new observability LogProvider, expanding PiiRedactionService guardrails, contributing operational runbooks, or fixing a bug, this guide will help you get started quickly.

Ways You Can Contribute

New Log Providers: Implement the LogProvider interface for platforms like Datadog, AWS CloudWatch Logs, Splunk, New Relic, or Jaeger.

PII Redaction Rules: Add regex patterns and unit tests to PiiRedactionService for cloud provider keys (AWS, GCP, Azure), SSNs, or custom secret formats.

Operational Runbooks: Contribute realistic production troubleshooting guides under /runbooks/ with proper YAML frontmatter.

RAG & Reranking Enhancements: Improve chunking heuristics in MarkdownRunbookSplitter or hybrid scoring in RunbookRagService.

Local Development Setup

Fork and Clone the Repository

git clone https://github.com/<your-username>/sentinel-mcp.git
cd sentinel-mcp


Start Backing Infrastructure (PostgreSQL pgvector + Ollama)

docker compose up sentinel-postgres sentinel-ollama ollama-model-puller -d


Run the Test Suite
Ensure Docker is running (required for Testcontainers), then run:

./mvnw clean test


Start the Application Locally

./mvnw spring-boot:run \
-Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/sentinel_rag --spring.ai.ollama.base-url=http://localhost:11434"


How to Add a New LogProvider

Adding a new log source takes three steps:

Implement LogProvider inside src/main/java/com/akash/sentinel/provider/:

@Component
@ConditionalOnProperty(name = "sentinel.log-provider", havingValue = "datadog")
public class DatadogLogProvider implements LogProvider {
@Override
public List<LogSpanDto> queryByTraceId(String traceId) {
// Fetch and map spans using defensive RestClient timeouts
}

    @Override
    public List<LogSpanDto> queryRecentErrors(int minutesBack) {
        // Fetch recent error spans
    }
}


Add Configuration Properties in application.yaml under sentinel.<provider-name>.

Write Unit Tests in src/test/java/com/akash/sentinel/provider/ using MockRestServiceServer or WireMock to verify JSON parsing and timeout handling.

Pull Request Checklist

Before opening a Pull Request, please verify:

[ ] ./mvnw clean test passes with zero failures.

[ ] Any new LogProvider uses defensive timeouts (3s connect / 5s read) and degrades gracefully on network errors.

[ ] STDIO purity is preserved: never use System.out.println() in production code (use SLF4J Logger so stdio mode stays clean for JSON-RPC).

[ ] Documentation (README.md) is updated if new environment variables or properties were introduced.