# Sentinel MCP

A Model Context Protocol (MCP) server for intelligent incident triage and runbook retrieval using RAG (Retrieval-Augmented Generation).

## Overview

Sentinel MCP provides AI-powered incident analysis by:
- Ingesting and indexing operational runbooks with semantic chunking
- Analyzing error spikes and log spans for incident triage
- Retrieving relevant runbooks based on incident context
- Redacting PII (Personally Identifiable Information) from logs
- Providing hybrid RAG with metadata filtering and reranking

## Milestones

- Milestone 1 [COMPLETED] (v0.1.0): Foundation, Guardrails & Mock Strategy
- Milestone 2 [COMPLETED] (v0.2.0): Semantic Markdown Chunker & Hybrid RAG
  - `MarkdownRunbookSplitter` — H2 semantic chunking with metadata inheritance
  - `RunbookIngestionRunner` — idempotent startup vector indexing
  - `RunbookRagService` — metadata filter + 0.72 threshold + 70/30 hybrid reranker
  - 3 Markdown runbooks with YAML frontmatter in `/runbooks`

## Tech Stack

- **Java 21**
- **Spring Boot 4.1.1**
- **Spring AI 2.0.1** — MCP server, Ollama integration, pgvector store
- **PostgreSQL 16** with pgvector extension for vector embeddings
- **Ollama** — Local LLM and embedding model (nomic-embed-text)
- **Testcontainers** — Integration testing with PostgreSQL and Ollama

## Prerequisites

- Docker and Docker Compose
- Java 21
- Maven 3.9+

## Quick Start

1. **Start infrastructure services:**
   ```bash
   docker-compose up -d
   ```

   This starts:
   - PostgreSQL with pgvector on port 5432
   - Ollama on port 11434
   - Auto-pulls the `nomic-embed-text` embedding model

2. **Build the application:**
   ```bash
   ./mvnw clean package
   ```

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

   The application will:
   - Connect to PostgreSQL and Ollama
   - Ingest runbooks from `/runbooks` directory
   - Index them in the vector store
   - Start the MCP server on the configured port

## Configuration

Key configuration properties (in `application.properties` or `application.yml`):

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
    vectorstore:
      pgvector:
        index-type: HNSW
        dimension: 768
  datasource:
    url: jdbc:postgresql://localhost:5432/sentinel_rag
    username: postgres
    password: secret
```

## Project Structure

```
sentinel-mcp/
├── src/main/java/com/akash/sentinel/
│   ├── mcp/dto/              # MCP data transfer objects
│   ├── provider/             # Log providers (MockChaosLogProvider)
│   ├── rag/                  # RAG services
│   │   ├── MarkdownRunbookSplitter.java
│   │   ├── RunbookIngestionRunner.java
│   │   └── RunbookRagService.java
│   ├── service/              # Business services
│   │   ├── PiiRedactionService.java
│   │   └── TraceAnalyzerService.java
│   └── SentinelMcpApplication.java
├── runbooks/                 # Markdown runbooks with YAML frontmatter
└── docker-compose.yml        # Infrastructure services
```

## MCP Tools

The server exposes the following MCP tools:

- `analyze_error_spike` — Analyze error rate spikes from logs
- `triage_incident` — Generate triage reports from log spans
- `search_runbooks` — Retrieve relevant runbooks using RAG
- `redact_pii` — Redact PII from log data

## Runbook Format

Runbooks should be Markdown files with YAML frontmatter:

```markdown
---
title: Database Connection Pool Exhaustion
severity: high
services:
  - api
  - database
tags:
  - performance
  - database
---

# Database Connection Pool Exhaustion

## Symptoms
...
```

## Development

### Running Tests

```bash
./mvnw test
```

Tests use Testcontainers for PostgreSQL and Ollama integration.

### Adding New Runbooks

1. Create a new Markdown file in `/runbooks`
2. Add YAML frontmatter with metadata
3. Restart the application to trigger ingestion

## License

[Specify your license here]
