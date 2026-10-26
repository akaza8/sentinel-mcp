sentinel-mcp/
├── docker-compose.yml                        # pgvector + Ollama local runtime
├── pom.xml                                   # Maven dependencies
├── runbooks/                                 # Markdown runbooks indexed on startup
│   ├── RB-001-hikaricp-pool-exhausted.md
│   ├── RB-002-stripe-gateway-timeout.md
│   └── RB-003-redis-cache-connection-refused.md
└── src/
├── main/
│   ├── java/com/akash/sentinel/
│   │   ├── SentinelMcpApplication.java
│   │   ├── config/
│   │   │   ├── RestClientConfig.java     # HTTP client timeouts for Loki/Elastic
│   │   │   └── SentinelProperties.java   # @ConfigurationProperties("sentinel")
│   │   ├── mcp/
│   │   │   ├── IncidentTriageTools.java  # @McpTool endpoints
│   │   │   ├── RunbookResources.java     # @McpResource endpoints
│   │   │   └── dto/
│   │   │       ├── TriageReport.java     # Structured diagnosis returned to LLM
│   │   │       ├── LogSpanDto.java       # Single chronological log span
│   │   │       ├── ErrorSpikeDto.java    # Aggregated error summary
│   │   │       └── RunbookMatchDto.java  # Matched runbook & hybrid score
│   │   ├── service/
│   │   │   ├── TriageOrchestratorService.java
│   │   │   ├── PiiRedactionService.java  # Regex scrubber for JWTs, emails, keys
│   │   │   └── TraceAnalyzerService.java # Root-cause span isolation & query normalization
│   │   ├── provider/                     # STRATEGY PATTERN
│   │   │   ├── LogProvider.java          # Core interface
│   │   │   ├── MockChaosLogProvider.java # @ConditionalOnProperty("mock")
│   │   │   ├── LokiLogProvider.java      # @ConditionalOnProperty("loki")
│   │   │   └── ElasticLogProvider.java   # @ConditionalOnProperty("elasticsearch")
│   │   └── rag/
│   │       ├── MarkdownRunbookSplitter.java # Frontmatter parser + H2 semantic chunker
│   │       ├── RunbookIngestionRunner.java  # Idempotent startup vector indexer
│   │       └── RunbookRagService.java       # Hybrid search + 70/30 reranker
│   └── resources/
│       ├── application.yml               # Default config (Streamable HTTP + Mock Provider)
│       └── application-stdio.yml         # STDIO profile for local IDE subprocess
└── test/
└── java/com/akash/sentinel/
├── MarkdownRunbookSplitterTest.java
├── PiiRedactionServiceTest.java
└── RunbookRagIntegrationTest.java # Testcontainers pgvector verification