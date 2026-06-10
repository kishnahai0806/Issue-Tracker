# Security Baseline

This project is a production-grade, multi-tenant issue tracker. Step 1 establishes guardrails only; full authentication, authorization, schema, and business workflows are deferred to later implementation phases.

## OWASP Top 10 Coverage Strategy

- Broken Access Control: enforce tenant membership and object-level authorization for every organization, project, issue, comment, attachment, analytics, and admin operation.
- Cryptographic Failures: store secrets outside committed files, validate JWT signatures and claims, hash refresh tokens at rest, and require TLS in production.
- Injection: use validation, parameterized repository queries, and Liquibase-managed schema changes.
- Insecure Design: design every workflow around tenant isolation, least privilege, idempotency, and after-commit side effects.
- Security Misconfiguration: keep Swagger disabled by default, expose only safe actuator health probes publicly, and configure CORS through explicit origins.
- Vulnerable and Outdated Components: use the Maven `security-scan` profile for OWASP Dependency-Check and add SCA to CI/CD later.
- Identification and Authentication Failures: validate JWT subject, issuer, audience, signature, and expiration; deny disabled users; rotate and hash refresh tokens.
- Software and Data Integrity Failures: pin Docker image versions, avoid `latest`, and document any unpinned CI action risk.
- Security Logging and Monitoring Failures: use structured logs without sensitive values and include trace identifiers.
- Server-Side Request Forgery: validate external storage endpoints and avoid accepting arbitrary backend fetch URLs.

## Broken Access Control and IDOR

- Every organization, project, issue, comment, attachment, and analytics request must validate tenant membership.
- Path IDs are not authorization. They are only identifiers.
- A future `OrganizationAccessValidator` must enforce object-level authorization before returning or mutating tenant data.
- Planned audit history uses one model: `issue_audit_log`.
- Later code should introduce `IssueAuditLog` and `IssueAuditLogRepository`.
- `IssueDetailResponse.auditHistory` must read from `issue_audit_log`.

## CORS

- Production must never use wildcard `*` origins.
- Java config reads allowed origins from `app.cors.allowed-origins`.
- `ALLOWED_ORIGINS` must be explicit and environment-specific.
- Local development may allow only localhost origins.
- The baseline security config rejects empty or wildcard CORS origins.

## Authentication

- JWTs must include and validate issuer and audience.
- JWT validation must check expiration, signature, subject, issuer, and audience.
- Disabled users must not be able to access APIs or refresh tokens.
- Refresh tokens must be hashed at rest and never stored raw.
- Full JWT authentication is not implemented in Step 1.

## Rate Limiting

- Auth endpoints must use Bucket4j when implemented.
- Limits must be configurable under `app.security.rate-limiting.auth`.
- Hardcoded rate limits are not acceptable for production behavior.

## Actuator

- Health, readiness, and liveness may be public.
- Prometheus must not be exposed publicly to the internet.
- Local Prometheus scraping `:8090/actuator/prometheus` is for local development only.
- Do not permit all actuator endpoints in Spring Security.

## Swagger and OpenAPI

- Swagger UI must not be publicly enabled in production.
- `springdoc.swagger-ui.enabled` and API docs are disabled by default.
- Local profile may enable Swagger for development only.

## Secrets

- Do not commit real secrets in `.env`, `application-local.yml`, `application-local.yaml`, Docker Compose production files, or Kubernetes YAML.
- `.env.example` must contain fake local sample values only.
- JWT secrets, storage credentials, mail credentials, database passwords, and Grafana credentials must come from environment variables or a secrets manager.

## Docker and Compose

- `docker-compose.yml` is local-only infrastructure.
- Do not create `docker-compose.prod.yml` with hardcoded database credentials.
- Grafana admin credentials come from environment variables without hardcoded Compose fallbacks.
- Redis without auth is local-only.
- Production Redis must use authentication, TLS, or private network controls.
- Docker image versions must be pinned.
- MinIO uses a pinned image tag.

## TLS

- Production documentation must not imply plaintext HTTP is acceptable.
- Local and minikube demos may use local ingress behavior.
- Production deployments must document TLS termination and secure upstream routing.

## Logging and PII

- Do not log passwords, access tokens, refresh tokens, authorization headers, raw JWTs, or PII such as user email in error logs.
- Structured logs should prefer identifiers such as `userId`, `organizationId`, `projectId`, `issueId`, and `traceId`.
- Use `@Slf4j` if logging is added.

## Supply Chain

- Pin Docker image versions and avoid `latest`.
- Run `.\mvnw.cmd -Psecurity-scan dependency-check:check` for OWASP Dependency-Check when network access is available.
- Normal compile must not depend on slow vulnerability database downloads.
- CI/CD must include SCA in a later phase.
- Later GitHub Actions should use SHA-pinned actions or document the accepted risk.

## Kafka

- This project does not use Kafka.
- Do not add Kafka in this scaffold step.
- The Project 1 Kafka PLAINTEXT finding is not directly applicable here.
- If messaging is added later, production messaging must not use unauthenticated plaintext transport.

## Security Testing Plan

- Later phases must include tests for IDOR and tenant isolation.
- Disabled users must be blocked from access and token refresh.
- Auth failures must return correct status codes, including `/error` redispatch behavior.
- CORS must reject wildcard and unapproved origins.
- JWT issuer and audience validation must be covered.
- Refresh token hashing must be covered.
- Actuator exposure must be covered.
- Auth endpoint rate limiting must be covered.

## Servlet Filters

- Filters managed by Spring Security must not also auto-register in the servlet container.
- Any filter bean later inserted into the security chain must be disabled with `FilterRegistrationBean`.
- If multiple filters are added before the same Spring Security anchor, ordering must be intentional because the last added filter runs first.

## Events and Idempotency

- Issue creation must accept an `Idempotency-Key` header in a later phase.
- Idempotency state should be backed by Redis and use the configured TTL.
- WebSocket broadcasts must come from domain or application events handled by `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- Events must not broadcast uncommitted transaction state.

## OpenTelemetry

- The app exports traces to the OTel Collector endpoint.
- The app must not export traces directly to Jaeger.
- The local collector exports traces to Jaeger for developer inspection.
