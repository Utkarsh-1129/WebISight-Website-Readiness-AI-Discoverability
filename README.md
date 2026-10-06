# AI Readiness Audit — Web Application

A production Spring Boot web application that audits any website for
**AI discoverability** (can AI assistants find, cite, and correctly
represent this site?) and **user engagement** (is the page actually
clear and usable?).

Enter a URL in the browser, get back a scored, evidence-backed report in
seconds — no CLI, no JSON file to read by hand.

## Architecture

```
Browser (index.html)
     │  POST /api/audit  { "url": "..." }
     ▼
AuditController  ──▶  AuditService  ──▶  AuditOrchestrator
                          │                    │
                    (bounded thread pool,      ├──▶ CrawlSkill
                     hard timeout)             ├──▶ SchemaSkill
                          │                    ├──▶ FreshnessSkill
                          ▼                    └──▶ EngagementSkill
                    ReportGenerator
                          │
                          ▼
                    AuditReport (JSON)
                          │
                          ▼
                 Rendered dashboard (browser)
```

Every skill runs against **one shared crawl** of the page (`HttpFetcher`
fetches it exactly once) — nobody re-fetches the site.

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3 (Spring MVC, embedded Tomcat) |
| Build | Maven |
| Frontend | Plain HTML, CSS, vanilla JavaScript (no framework, no build step) |
| HTML parsing | Jsoup |
| JSON | Jackson (via Spring's built-in integration) |
| Validation | Jakarta Bean Validation |
| Monitoring | Spring Boot Actuator (`/actuator/health`) |
| Testing | JUnit 5, Mockito, Spring MockMvc |

## Project structure

```
ai-readiness-webapp/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── src/main/java/com/readinessaudit/webapp/
│   ├── AiReadinessWebApplication.java   Spring Boot entrypoint
│   ├── controller/
│   │   └── AuditController.java         REST API: POST /api/audit
│   ├── service/
│   │   └── AuditService.java            Timeout enforcement, exception translation
│   ├── config/
│   │   ├── AsyncConfig.java             Bounded thread pool for audits
│   │   └── RateLimitFilter.java         Per-IP rate limiting on /api/audit
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  Consistent JSON error responses
│   │   ├── InvalidUrlException.java
│   │   ├── AuditTimeoutException.java
│   │   └── AuditFailedException.java
│   ├── dto/
│   │   ├── AuditRequest.java            Validated request body
│   │   └── ErrorResponse.java           Consistent error JSON shape
│   ├── crawler/
│   │   ├── HttpFetcher.java             Fetches + parses a page (Jsoup)
│   │   └── CrawlResult.java             Shared crawl data model
│   ├── skills/
│   │   ├── Skill.java                   Common interface
│   │   ├── CrawlSkill.java              Crawlability & rendering
│   │   ├── SchemaSkill.java             JSON-LD & metadata
│   │   ├── FreshnessSkill.java          Stale/consistent facts
│   │   └── EngagementSkill.java         UX & content clarity
│   ├── orchestrator/
│   │   └── AuditOrchestrator.java       Runs all skills against one crawl
│   └── report/
│       ├── Finding.java                 One evidence-backed issue
│       ├── AuditReport.java             Final JSON shape
│       └── ReportGenerator.java         Scoring + prioritization
├── src/main/resources/
│   ├── application.yml                  Externalized configuration
│   └── static/
│       ├── index.html
│       ├── css/style.css
│       └── js/app.js
└── src/test/java/com/readinessaudit/webapp/
    ├── report/ReportGeneratorTest.java     Scoring logic (unit)
    ├── service/AuditServiceTest.java       Timeout & error handling (Mockito)
    └── controller/AuditControllerTest.java Validation & API contract (MockMvc)
```

## Running it

Requires a JDK 21 and Maven with normal internet access (to pull
Spring Boot, Jsoup, and their dependencies from Maven Central on first
build).

```bash
mvn spring-boot:run
```

Then open **http://localhost:8080** in a browser and enter a URL.

Or build and run the jar directly:

```bash
mvn clean package
java -jar target/ai-readiness-webapp.jar
```

### Running with Docker

```bash
docker compose up --build
```

This builds the app in a Maven container, packages it into a slim JRE
runtime image running as a non-root user, and exposes it on port 8080
with a container healthcheck against `/actuator/health`.

## API

### `POST /api/audit`

```json
{ "url": "https://example.com" }
```

Returns an `AuditReport`:

```json
{
  "url": "https://example.com",
  "generatedAt": "2026-09-26T12:00:00Z",
  "overallScore": 90,
  "skillScores": {
    "crawl-render-audit": 100,
    "schema-audit": 82,
    "freshness-audit": 90,
    "engagement-audit": 90
  },
  "summary": { "totalChecks": 9, "passed": 6, "failed": 3, "critical": 0, "high": 1, "medium": 2, "low": 0 },
  "prioritizedActions": [ { "skill": "...", "severity": "HIGH", "issue": "...", "evidence": "...", "recommendation": "..." } ],
  "findingsBySkill": { "...": "every finding, grouped by skill" }
}
```

`prioritizedActions` is sorted worst-severity-first.

**Error responses** (400, 429, 502, 504, 500) all share one shape:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "url must start with http:// or https://" }
```

### `GET /api/ping`

Simple liveness check for the API layer itself.

### `GET /actuator/health`

Spring Boot Actuator health endpoint, used by the Docker healthcheck and
suitable for a load balancer or orchestrator's readiness probe.

## Production considerations already built in

- **Bounded thread pool + hard timeout** (`AsyncConfig`, `AuditService`) — a
  slow or hanging target site can never tie up a request indefinitely or
  starve the web server's own request-handling threads. Configurable via
  `AUDIT_TIMEOUT_SECONDS` and `AUDIT_EXECUTOR_POOL_SIZE`.
- **Per-IP rate limiting** (`RateLimitFilter`) on `/api/audit` specifically,
  since each request triggers a real outbound network call and is more
  expensive than a typical API call. Configurable via
  `AUDIT_RATE_LIMIT_PER_MINUTE`. Single-instance only — a multi-instance
  deployment behind a load balancer would need a shared store (e.g. Redis)
  instead.
- **Centralized, consistent error handling** (`GlobalExceptionHandler`) — no
  raw stack traces ever reach the client; every failure path returns the
  same JSON error shape with an appropriate HTTP status.
- **Input validation** (`AuditRequest`) — rejects blank or non-http(s) URLs
  before any network call is attempted.
- **Externalized configuration** (`application.yml`) — every tunable
  (timeout, pool size, rate limit, port) is overridable via environment
  variable without a rebuild, for different deployment environments.
- **Structured logging** — every audit logs its start, duration, and
  outcome; failures are logged with context before being translated into
  a client-safe error message.
- **Health endpoint** for container/load-balancer readiness and liveness
  probes.
- **Non-root container user** in the Docker image.

## Testing

```bash
mvn test
```

- `ReportGeneratorTest` — verifies scoring math and prioritization
  ordering against synthetic findings, no network required.
- `AuditServiceTest` — verifies the timeout and exception-translation
  behavior using Mockito, including a real timeout firing under a tight
  time budget.
- `AuditControllerTest` — verifies request validation and the API
  contract using Spring's `MockMvc`, with the service layer mocked out.

## Extending with a new skill

1. Implement `com.readinessaudit.webapp.skills.Skill` in a new class
   annotated `@Component`.
2. That's it — Spring auto-discovers every `Skill` bean and injects the
   full list into `AuditOrchestrator`. No other code changes needed.

## What's deliberately out of scope for this version

- **Real JavaScript rendering.** `CrawlSkill` uses a raw-HTML word-count
  heuristic instead of executing JS (e.g. via a headless browser), which
  would add a heavyweight runtime dependency. Most AI crawlers don't
  execute JavaScript either, so this is a reasonably faithful simulation.
- **Multi-page/whole-site crawling.** The API audits one URL per request
  by design; a sitemap-wide crawler is a materially different tool with
  different failure modes (infinite loops, politeness delays).
- **Persistent storage / audit history.** Every request is stateless;
  nothing is written to a database. Add one if you need to track scores
  over time.
