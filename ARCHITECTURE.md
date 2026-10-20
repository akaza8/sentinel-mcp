# Project Specification: Sentinel-MCP (Standalone Real-Time Incident & Log Triage MCP Server)

## 1. Executive Summary & Problem Statement
In microservice architectures, resolving production incidents suffers from a severe "Triage & Context-Switching Tax." Engineers must leave their IDE, query centralized logging dashboards (Elasticsearch/Kibana, Grafana Loki, Datadog) using distributed `traceId` strings, identify the root failing span, and manually search internal wikis/runbooks for past remediation steps.

**Sentinel-MCP** is a standalone, real-time **Model Context Protocol (MCP) Server** built with **Java 21, Spring Boot, and Spring AI 2.0**. It connects an engineer's IDE assistant (IntelliJ Continue.dev, Cursor, Claude Desktop) directly to live log providers via the Strategy Pattern and correlates exceptions against a **PostgreSQL (`pgvector`)** knowledge base of Markdown runbooks and past incident post-mortems.

---

## 2. Core Architectural Principles
1. **100% Standalone & Non-Intrusive:** Sentinel-MCP does NOT store or duplicate company logs. It queries existing enterprise log stores (Elasticsearch, Grafana Loki, Postgres, or a built-in Mock Chaos Provider for local demos) on-demand in real time via a pluggable `LogProvider` interface.
2. **Zero-Impact on Business Transactions:** Operates strictly as an out-of-band, read-only investigator.
3. **PII & Token Guardrails:** All log lines retrieved from external providers pass through an in-memory `PiiRedactionService` (scrubbing JWTs, API keys, emails, credit cards) and an error-normalization filter before being embedded or returned to the LLM context window.
4. **Deterministic Hybrid RAG:** Avoids naive fixed-size chunking. Uses Markdown H2 (`##`) semantic splitting, metadata inheritance, error-signature normalization, and a hybrid scoring formula combining vector similarity with exact exception class matching.
5. **Zero-API-Key Local Reproducibility:** Uses **Docker Compose** with **PostgreSQL (`pgvector`)** and **Ollama (`nomic-embed-text`)** so any developer or recruiter can clone and test the entire RAG + MCP pipeline locally without paid API keys.

---

## 3. High-Level System Architecture

```text
[ Developer IDE: IntelliJ (Continue.dev) / Cursor / Claude Desktop ]
                                │
                                │ JSON-RPC 2.0 (Streamable HTTP "/mcp" or STDIO)
                                ▼
================================================================================
                     SENTINEL-MCP (Spring Boot Application)
================================================================================
  [ MCP Entrypoint Layer ]
   ├── IncidentTriageTools (@McpTool)
   ├── RunbookResources    (@McpResource)
   └── IncidentPrompts     (@McpPrompt)
                                │
                                ▼
  [ Core Orchestration & Guardrail Layer ]
   └── TriageOrchestratorService
        ├── 1. Queries LogProvider (Strategy Pattern)
        ├── 2. Sanitizes via PiiRedactionService (Regex JWT/Email/Key Scrubber)
        ├── 3. Isolates Root Cause Span & Normalizes Error Signature Tuple
        └── 4. Queries RunbookRagService (Metadata Filter + Vector Search + Rerank)
               │                                           │
               ▼ (Live Read-Only Query)                    ▼ (Cosine Similarity)
==========================================     =================================
   EXTERNAL LOG PROVIDERS (Pluggable)               LOCAL RAG KNOWLEDGE BASE
==========================================     =================================
 - MockChaosLogProvider (Default Demo)          - PostgreSQL 16 + pgvector
 - LokiLogProvider      (Grafana LogQL)         - Ollama (nomic-embed-text, 768d)
 - ElasticLogProvider   (Elasticsearch)         - H2-Chunked /runbooks/*.md
 - JdbcPostgresProvider (SQL Table Reader)
```
