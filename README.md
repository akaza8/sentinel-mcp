Sentinel-MCP

Autonomous Incident Triage & Hybrid RAG Runbook Server via Model Context Protocol (MCP).

Sentinel-MCP is a Spring Boot server that connects AI coding assistants such as Claude Desktop, Cursor, and IntelliJ Continue.dev to observability data and operational runbooks.

When an incident occurs, Sentinel-MCP retrieves distributed error traces, redacts sensitive information, identifies the likely root cause, and performs a 70/30 hybrid RAG search over PostgreSQL with pgvector. It returns structured incident details and relevant remediation instructions.
Table of Contents
1. How the Triage Pipeline Works
2. System Architecture
3. Quick Start & Environment Setup
   - Option A: Dockerized Setup
   - Option B: Local Development
4. Selecting & Switching Log Providers
5. Connecting to AI Clients
6. Exposed MCP Tools & Test Prompts
7. Adding Custom Runbooks & Deduplication
8. Project Structure
9. Contributing
10. License
How the Triage Pipeline Works
Sentinel-MCP automates incident investigation in four stages:
1. Pluggable Log Retrieval (LogProvider)
   Fetches raw trace spans by traceId or recent error windows from Grafana Loki, Elasticsearch, or the built-in Mock Chaos Engine. The mock provider is the default when no provider is configured (matchIfMissing = true).
2. PII Sanitization (PiiRedactionService)
   Uses compiled regular expressions to redact sensitive values such as JWTs, Bearer tokens, API keys, email addresses, and IPv4 addresses before log data reaches the embedding model or LLM context.
3. Root-Cause Isolation (TraceAnalyzerService)
   Analyzes distributed spans across services to identify the likely originating service and primary exception signature, such as java.sql.SQLTransientConnectionException.
4. Hybrid RAG Matching (RunbookRagService)
   Embeds the error signature using Ollama (nomic-embed-text) and searches PostgreSQL pgvector using an HNSW cosine index. The ranking combines:
   - 70% semantic vector similarity
   - 30% exact metadata match, based on runbook metadata such as service and primary_exception
System Architecture
  AI Client (Claude Desktop / Cursor / IntelliJ Continue.dev)
       |
       | JSON-RPC 2.0
       | Streamable HTTP (:8080) or STDIO
       v
+--------------------------------------------------------------------+
|              Sentinel-MCP Server (Spring Boot / Java 21)           |
|                                                                    |
|  +----------------------+      +-------------------------------+  |
|  | MCP Tool Router      |----->| LogProvider                    |  |
|  | Trace Analyzer       |      | Mock / Loki / Elasticsearch    |  |
|  +----------------------+      +-------------------------------+  |
|             |                                                      |
|             v                                                      |
|  +----------------------+      +-------------------------------+  |
|  | PiiRedactionService  |----->| RunbookRagService               |  |
|  +----------------------+      +-------------------------------+  |
+--------------------------------------------------------------------+
                  |                              |
                  v                              v
       +------------------------+      +--------------------------+
       | PostgreSQL 16          |      | Ollama                   |
       | pgvector + HNSW index  |      | nomic-embed-text         |
       +------------------------+      +--------------------------+
Quick Start & Environment Setup
You can run Sentinel-MCP entirely in Docker or run the Spring Boot application locally while using Docker for its infrastructure dependencies.
Option A: Dockerized Setup
Prerequisites: Docker and Docker Compose.
# 1. Clone the repository
git clone https://github.com/akaza8/sentinel-mcp.git
cd sentinel-mcp

# 2. Build and start PostgreSQL, Ollama, the model puller, and Sentinel-MCP
docker compose up -d --build

# 3. Check the application health
curl http://localhost:8080/actuator/health
The Docker Compose setup is intended to start the following services:
- sentinel-postgres on port 5432, initializing the sentinel_rag database.
- sentinel-ollama on port 11434.
- ollama-model-puller, which pulls the nomic-embed-text model.
- sentinel-mcp, which waits for its dependencies, ingests Markdown files from /runbooks, creates embeddings, and exposes the MCP server on http://localhost:8080.
Option B: Local Development
Prerequisites:
- Docker and Docker Compose
- Java 21+
- Maven 3.9+ (or use the included Maven wrapper)
The default application.yaml uses Docker service hostnames (sentinel-postgres and sentinel-ollama). When running the application directly on your host machine, override those URLs with localhost.
# 1. Start only the infrastructure containers
docker compose up -d sentinel-postgres sentinel-ollama ollama-model-puller

# 2. Run the test suite
./mvnw clean test

# 3. Start the application locally with host URL overrides
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/sentinel_rag --spring.ai.ollama.base-url=http://localhost:11434"
Selecting & Switching Log Providers
Sentinel-MCP uses Spring Boot's @ConditionalOnProperty with sentinel.log-provider to select the active log source at startup. Switching providers does not require Java code changes.
Provider value	Active implementation	Description	Default target URL
mock (default)	MockChaosLogProvider	Built-in deterministic test traces (matchIfMissing = true)	In-memory fixtures
loki	LokiLogProvider	Queries Grafana Loki using LogQL (/loki/api/v1/query_range)	http://localhost:3100
elasticsearch	ElasticsearchLogProvider	Queries Elasticsearch using its search DSL (/_search)	http://localhost:9200


1. Docker Compose environment variables
Ensure your docker-compose.yml passes the relevant environment variables to the sentinel-mcp service:
environment:
  - SPRING_PROFILES_ACTIVE=default
  - SENTINEL_LOG_PROVIDER=${SENTINEL_LOG_PROVIDER:-mock}
  - SENTINEL_LOKI_URL=${SENTINEL_LOKI_URL:-http://localhost:3100}
  - SENTINEL_ELASTICSEARCH_URL=${SENTINEL_ELASTICSEARCH_URL:-http://localhost:9200}
Then select a provider when starting the stack:
# Use Grafana Loki
SENTINEL_LOG_PROVIDER=loki \
SENTINEL_LOKI_URL=http://loki:3100 \
docker compose up -d

# Use Elasticsearch
SENTINEL_LOG_PROVIDER=elasticsearch \
SENTINEL_ELASTICSEARCH_URL=http://elasticsearch:9200 \
docker compose up -d
2. JAR startup arguments
java -jar target/sentinel-mcp-1.0.0.jar \
  --sentinel.log-provider=loki \
  --sentinel.loki.url=http://localhost:3100
3. AI client configuration
Set SENTINEL_LOG_PROVIDER to loki or elasticsearch in the env block of your MCP client configuration, or pass it as a Docker -e argument.
4. Application configuration
You can also configure the provider directly in src/main/resources/application.yaml:
sentinel:
  log-provider: mock # mock | loki | elasticsearch
  loki:
    url: http://localhost:3100
  elasticsearch:
    url: http://localhost:9200
Connecting to AI Clients
Sentinel-MCP supports two transport modes:
- Streamable HTTP: The default profile exposes the server at http://localhost:8080.
- STDIO: The stdio profile uses application-stdio.yaml, disables the web application, turns off the console banner, and writes server logs to /logs/sentinel-stdio.log so standard output remains available for JSON-RPC messages.
Add the relevant configuration to the MCP configuration file for your client:
- Claude Desktop: claude_desktop_config.json
- Cursor: .cursor/mcp.json
- IntelliJ Continue.dev: ~/.continue/config.json
Method 1: Run STDIO using Docker
This method requires the sentinel-postgres and sentinel-ollama containers to be running.
docker compose up -d sentinel-postgres sentinel-ollama ollama-model-puller
Add this server entry to your MCP client configuration:
{
  "mcpServers": {
    "sentinel-mcp": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--network", "host",
        "-e", "SPRING_PROFILES_ACTIVE=stdio",
        "-e", "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sentinel_rag",
        "-e", "SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434",
        "-e", "SENTINEL_LOG_PROVIDER=mock",
        "sentinel-mcp:latest"
      ]
    }
  }
}
Method 2: Run STDIO using a local JAR
Build the JAR first:
./mvnw clean package -DskipTests
Then add this entry to your MCP client configuration. Replace /absolute/path/to/sentinel-mcp with the actual absolute path to your project:
{
  "mcpServers": {
    "sentinel-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/sentinel-mcp/target/sentinel-mcp-1.0.0.jar"
      ],
      "env": {
        "SPRING_PROFILES_ACTIVE": "stdio",
        "SPRING_DATASOURCE_URL": "jdbc:postgresql://localhost:5432/sentinel_rag",
        "SPRING_AI_OLLAMA_BASE_URL": "http://localhost:11434",
        "SENTINEL_LOG_PROVIDER": "mock"
      }
    }
  }
}
Exposed MCP Tools & Test Prompts
Once connected, your AI assistant can access these MCP tools:
Tool	Input arguments	Purpose
triage_incident	traceId (String)	Fetches trace spans from the active log provider, redacts PII, identifies the root exception, and returns a structured TriageReport with matching remediation steps.
analyze_error_spike	minutesBack (int)	Queries recent error windows, groups exception signatures, and identifies possible cascading failures.
search_runbooks	query (String), service (String)	Searches pgvector using the hybrid semantic and metadata ranking strategy.
redact_pii	rawLogText (String)	Redacts tokens, email addresses, credit card numbers, and secrets from raw log snippets.


Built-in mock traces
With SENTINEL_LOG_PROVIDER=mock, try these prompts:
- Database connection pool issue:
  “Triage incident trace-db-901 and give me the exact remediation steps.”
  Simulates HikariCP connection pool starvation (SQLTransientConnectionException) in order-service.
- Stripe gateway timeout:
  “Why is trace-stripe-504 failing?”
  Simulates a Stripe payment gateway 504 timeout (ApiConnectionException) in stripe-gateway.
- Redis connection issue:
  “Investigate trace-redis-772.”
  Simulates Redis cluster connection refusal (RedisConnectionFailureException) in cache-service.
Adding Custom Runbooks & Deduplication
Runbooks are Markdown files stored in /runbooks/. They are ingested at application startup by RunbookIngestionRunner.
1. Add a Markdown runbook
Add YAML frontmatter at the top of the file. The metadata is attached to each H2 (##) section by MarkdownRunbookSplitter.

Example: /runbooks/kafka-consumer-lag.md
---
runbook_id: RB-004
title: Kafka Consumer Lag & Rebalance Storm
service: notification-service
primary_exception: org.apache.kafka.clients.consumer.CommitFailedException
severity: high
---

## Symptoms

Consumer group repeatedly enters `PREPARING_REBALANCE` and processing latency spikes.

## Remediation

1. Increase `spring.kafka.consumer.properties.max.poll.interval.ms` to `600000`.
2. Reduce `max.poll.records` from `500` to `100` to bound batch execution time.
2. Duplicate vector prevention
Sentinel-MCP uses deterministic IDs to make runbook ingestion idempotent:
1. Each chunk's document ID is derived with UUID.nameUUIDFromBytes(...), based on the runbook identifier and section header.
2. When vectorStore.add(documents) writes to PostgreSQL pgvector, an existing chunk is updated through a primary-key UPSERT rather than inserted as a duplicate.
This prevents duplicate vector entries when the application restarts and ingests the same runbooks again.
Project Structure
sentinel-mcp/
├── runbooks/                        # Operational Markdown runbooks with YAML frontmatter
├── src/
│   ├── main/
│   │   ├── java/com/akash/sentinel/
│   │   │   ├── mcp/                  # MCP tool endpoints and TriageReport DTOs
│   │   │   ├── provider/             # LogProvider interface and implementations
│   │   │   ├── rag/                  # Runbook splitting, ingestion, and RAG search
│   │   │   ├── service/              # Trace analysis and PII redaction
│   │   │   └── SentinelMcpApplication.java
│   │   └── resources/
│   │       ├── application.yaml      # Default Streamable HTTP profile
│   │       └── application-stdio.yaml # STDIO profile
│   └── test/java/                    # Unit and Testcontainers integration tests
├── Dockerfile                        # Multi-stage Eclipse Temurin 21 build
├── docker-compose.yml                # PostgreSQL, Ollama, model puller, and app
├── CONTRIBUTING.md                   # Contributor guidelines
└── pom.xml
Contributing
Contributions are welcome, including:
- New LogProvider implementations (for example, Datadog, AWS CloudWatch, or OpenTelemetry Collector).
- Additional operational runbooks and templates.
- Improvements to triage, redaction, and retrieval.
Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for local setup instructions, STDIO logging rules, and pull request guidelines before submitting changes.
License
Distributed under the MIT License. See LICENSE for more information.
