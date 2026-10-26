# Sentinel MCP

> AI-powered incident triage and runbook retrieval via Model Context Protocol (MCP).

Sentinel MCP is a production-grade Spring Boot server that combines semantic RAG (Retrieval-Augmented Generation) with hybrid vector search to help operators triage incidents faster. It ingests operational runbooks, analyzes error spans, and retrieves context-aware remediation steps through an MCP-compatible interface.

---

## Features

- **Semantic Runbook Chunking** — H2-level markdown splitting with YAML frontmatter inheritance
- **Hybrid RAG Search** — Metadata-filtered vector search with 70/30 reranking and similarity thresholds
- **Multi-Transport MCP** — HTTP (default) and pure STDIO (`spring.ai.mcp.server.stdio`) for Claude Desktop, Cursor, and IntelliJ Continue
- **Log Provider Abstraction** — Pluggable `LogProvider` implementations: Mock (default), Loki, Elasticsearch
- **PII Redaction** — Built-in `PiiRedactionService` for safe log handling
- **Zero-Setup Packaging** — Multi-stage Docker build with bundled `/runbooks`; `docker compose up -d` requires no Java 21 installation

---

## Architecture

```
Client (Claude / Cursor / Continue)
  │
  ▼
MCP Transport (HTTP or STDIO)
  │
  ▼
Sentinel MCP Server (Spring Boot 4.1.1 / Java 21)
  ├── RunbookIngestionRunner  →  /runbooks  →  pgvector (HNSW)
  ├── RunbookRagService       →  Hybrid search + rerank
  ├── LogProvider             →  Mock / Loki / Elasticsearch
  ├── PiiRedactionService     →  Log sanitization
  └── TraceAnalyzerService    →  Error signature normalization
```

---

## Prerequisites

- Docker & Docker Compose
- (Optional) Java 21 + Maven 3.9+ for local development

---

## Quick Start

```bash
# 1. Start infrastructure + build + expose on :8080
docker compose up -d

# 2. Verify
curl http://localhost:8080/actuator/health
```

The `sentinel-mcp` service depends on `sentinel-postgres` and `ollama-model-puller`. The embedding model (`nomic-embed-text`) is pulled automatically.

---

## 1-Minute Plug & Play

### Zero-Setup Docker Startup

```bash
docker compose up -d
```

- `sentinel-postgres` (pgvector) on `5432`
- `sentinel-ollama` on `11434`
- `sentinel-mcp` on `8080`

### MCP Client Config (STDIO Transport)

The container can run in pure JSON-RPC STDIO mode (`application-stdio.yaml`). Logs are redirected to `/logs/sentinel-stdio.log` so stdout remains 100% clean for clients.

**Claude Desktop** (`claude_desktop_config.json`)
```json
{
  "mcpServers": {
    "sentinel": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--network", "host",
        "-e", "SPRING_PROFILES_ACTIVE=stdio",
        "sentinel-mcp:latest"
      ]
    }
  }
}
```

**Cursor** (`.cursor/mcp.json`)
```json
{
  "mcpServers": {
    "sentinel": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--network", "host",
        "-e", "SPRING_PROFILES_ACTIVE=stdio",
        "sentinel-mcp:latest"
      ]
    }
  }
}
```

**IntelliJ Continue.dev** (`~/.continue/config.json`)
```json
{
  "mcpServers": {
    "sentinel": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--network", "host",
        "-e", "SPRING_PROFILES_ACTIVE=stdio",
        "sentinel-mcp:latest"
      ]
    }
  }
}
```

---

## Configuration

`application.yaml` (default HTTP profile):

```yaml
server:
  port: 8080

spring:
  application:
    name: sentinel-mcp
  threads:
    virtual:
      enabled: true

  datasource:
    url: jdbc:postgresql://localhost:5432/sentinel_rag
    username: postgres
    password: secret
    driver-class-name: org.postgresql.Driver

  ai:
    ollama:
      base-url: http://localhost:11434
      embedding:
        options:
          model: nomic-embed-text
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 768
    mcp:
      server:
        name: sentinel-incident-triage
        version: 1.0.0
        type: SYNC
        protocol: STREAMABLE

sentinel:
  log-provider: mock
  loki:
    url: http://localhost:3100
  elasticsearch:
    url: http://localhost:9200
  rag:
    similarity-threshold: 0.0
    top-k: 4
```

`application-stdio.yaml` (pure STDIO):

```yaml
spring:
  main:
    web-application-type: none
    banner-mode: off
  ai:
    mcp:
      server:
        stdio: true

logging:
  pattern:
    console: ""
  file:
    name: /logs/sentinel-stdio.log
```

---

## Adding Runbooks

There is **no** dynamic upload endpoint. Runbooks are ingested at startup by `RunbookIngestionRunner` from the `/runbooks` directory.

1. Create a markdown file with YAML frontmatter:
   ```markdown
   ---
   title: Payment Gateway Timeout
   severity: high
   service: payment-service
   ---
   ## Symptoms
   ...
   ## Remediation
   ...
   ```

2. Place it in `/runbooks/` (bundled in the Docker image or mounted via volume).

3. Restart the service (or rebuild the image). The splitter (`MarkdownRunbookSplitter`) extracts frontmatter, splits on H2 headers, and indexes chunks into pgvector with inherited metadata.

---

## Log Sources & Ingestion

There is **no** user-facing `addLog()` method. Logs are queried from external sources via the `LogProvider` interface.

| Provider | Activation | Source |
|---|---|---|
| `MockChaosLogProvider` | `sentinel.log-provider=mock` (default, `matchIfMissing=true`) | Hard-coded mock traces for testing |
| `LokiLogProvider` | `sentinel.log-provider=loki` | Grafana Loki `/loki/api/v1/query_range` |
| `ElasticsearchLogProvider` | `sentinel.log-provider=elasticsearch` | Elasticsearch `/_search` |

Configure the endpoint in `application.yaml` (`sentinel.loki.url` / `sentinel.elasticsearch.url`). The providers use defensive `RestClient` timeouts (3s connect / 5s read).

To add custom logs, either:
- Configure your Loki/Elasticsearch instance and set the provider accordingly, or
- Extend `MockChaosLogProvider` with new trace cases for local development.

---

## Project Structure

```
sentinel-mcp/
├── src/main/java/com/akash/sentinel/
│   ├── mcp/dto/
│   ├── provider/          # LogProvider implementations
│   ├── rag/               # MarkdownRunbookSplitter, RunbookRagService
│   ├── service/           # PiiRedactionService, TraceAnalyzerService
│   └── SentinelMcpApplication.java
├── runbooks/              # Markdown runbooks with YAML frontmatter
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

---

## MCP Tools

- `analyze_error_spike`
- `triage_incident`
- `search_runbooks`
- `redact_pii`

---

## Development

```bash
# Build and test
./mvnw clean test

# Run locally (requires PostgreSQL + Ollama)
./mvnw spring-boot:run
```

---

## License

