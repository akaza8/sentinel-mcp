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
  - `RunbookRagService` — metadata filter + 0.55 threshold + 70/30 hybrid reranker
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

2. **Build the application:**
   ```bash
   ./mvnw clean package
   ```

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

## Configuration

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
│   ├── mcp/dto/
│   ├── provider/
│   ├── rag/
│   ├── service/
│   └── SentinelMcpApplication.java
├── runbooks/
└── docker-compose.yml
```

## MCP Tools

- `analyze_error_spike`
- `triage_incident`
- `search_runbooks`
- `redact_pii`

## Runbook Format

```markdown
---
title: Example
severity: high
---
```

## Development

```bash
./mvnw test
```

## License

Nooooooooo
