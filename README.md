# sentinel-mcp

## Milestones

- Milestone 1 [COMPLETED] (v0.1.0): Foundation, Guardrails & Mock Strategy
- Milestone 2 [COMPLETED] (v0.2.0): Semantic Markdown Chunker & Hybrid RAG
  - `MarkdownRunbookSplitter` — H2 semantic chunking with metadata inheritance
  - `RunbookIngestionRunner` — idempotent startup vector indexing
  - `RunbookRagService` — metadata filter + 0.72 threshold + 70/30 hybrid reranker
  - 3 Markdown runbooks with YAML frontmatter in `/runbooks`
