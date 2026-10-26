# Contributing to Sentinel MCP

Thank you for your interest in contributing. This document covers guidelines, best practices, and the workflow for submitting changes.

## Code of Conduct

Be respectful, constructive, and inclusive. Harassment or discrimination of any kind will not be tolerated.

## Getting Started

1. Fork the repository.
2. Clone your fork locally.
3. Ensure you have Java 21 and Maven 3.9+ installed.
4. Run `./mvnw clean test` to verify the baseline.

## Development Workflow

- Create a feature branch from `main`: `git checkout -b feat/your-feature`
- Make focused commits with clear messages.
- Add or update tests for any changed behavior.
- Ensure `docker compose up -d` works for integration validation.

## Code Standards

- Follow existing package structure (`com.akash.sentinel.*`).
- Use constructor injection; avoid field injection.
- Keep methods focused; prefer small, testable units.
- Add Javadoc for public APIs.
- Use `final` where appropriate.
- Handle nulls defensively (see `MockChaosLogProvider`, `LokiLogProvider`).

## Testing Requirements

- Unit tests for services (`PiiRedactionServiceTest`, etc.).
- Integration tests using Testcontainers (`RunbookRagIntegrationTest`).
- Verify `LogProvider` implementations remain mutually exclusive (`@ConditionalOnProperty`).

## Pull Request Process

1. Update `README.md` if user-facing behavior changes.
2. Update `docker-compose.yml` or `Dockerfile` if packaging changes.
3. Ensure the commit message follows the convention: `type: description` (e.g., `feat:`, `fix:`, `docs:`).
4. Request review from maintainers.
5. Confirm CI passes (build + tests).

## Best Practices

- **Configuration**: Use `application.yaml` profiles; never hardcode URLs in source.
- **Security**: Redact PII via `PiiRedactionService`; never log tokens or keys.
- **Performance**: Use `RestClient` with defensive timeouts; avoid blocking calls in async paths.
- **Observability**: Log at appropriate levels; redirect STDIO logs to `/logs/sentinel-stdio.log` when using `application-stdio.yaml`.
- **Runbooks**: Place new runbooks in `/runbooks` with YAML frontmatter; do not add dynamic upload endpoints.

## Questions?

Open a discussion or issue. For urgent bugs, include reproduction steps and `docker logs` output.
