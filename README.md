# WebISight — Website Readiness & AI Discoverability Analyzer

[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Live Demo](https://img.shields.io/badge/Live_Demo-Railway-0B0D0E?logo=railway&logoColor=white)](https://webisight-website-readiness-ai-discoverability-production.up.railway.app)

A production-grade **Spring Boot** web application that audits any website for **AI discoverability** (*can AI assistants find, cite, and accurately represent this site?*) and **user engagement** (*is the page clear, structured, and usable?*).

Enter a URL in the browser and receive a scored, evidence-backed diagnostic report in seconds — no CLI required and no raw JSON files to inspect by hand.

- **Live Deployment:** [webisight-website-readiness-ai-discoverability-production.up.railway.app](https://webisight-website-readiness-ai-discoverability-production.up.railway.app)
- **Source Code:** [github.com/Utkarsh-1129/WebISight-Website-Readiness-AI-Discoverability](https://github.com/Utkarsh-1129/WebISight-Website-Readiness-AI-Discoverability)

---

## Table of Contents

- [Key Capabilities](#key-capabilities)
- [System Architecture](#system-architecture)
- [Audit Skills & Evaluation Criteria](#audit-skills--evaluation-criteria)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run Locally with Maven](#run-locally-with-maven)
  - [Run with Docker Compose](#run-with-docker-compose)
- [Environment Configuration](#environment-configuration)
- [REST API Reference](#rest-api-reference)
- [Production Engineering Highlights](#production-engineering-highlights)
- [Testing](#testing)
- [Extending with a New Audit Skill](#extending-with-a-new-audit-skill)
- [Scope & Design Trade-offs](#scope--design-trade-offs)
- [Author](#author)

---

## Key Capabilities

- **Single-Pass Shared Crawling:** Fetches and parses the target HTML document once via `HttpFetcher` (`Jsoup`) and shares the immutable `CrawlResult` across all audit skills.
- **Modular Skill Pipeline:** Evaluates pages across four specialized dimensions: **Crawlability & Rendering**, **Structured Data & Schema**, **Content Freshness**, and **User Engagement**.
- **Fault-Tolerant Execution:** Executes audits inside a bounded thread pool with strict per-request timeouts so slow or unresponsive target servers never block web server threads.
- **Per-IP Rate Limiting:** Protects the outbound crawling endpoint (`/api/audit`) from abuse using a configurable sliding-window rate limiter.
- **Actionable Prioritization:** Automatically ranks failed checks by severity (`CRITICAL` → `HIGH` → `MEDIUM` → `LOW`) with concrete DOM evidence and remediation steps.

---

## System Architecture

```text
Browser (index.html)
     │  POST /api/audit  { "url": "[https://example.com](https://example.com)" }
     ▼
AuditController  ──▶  AuditService  ──▶  AuditOrchestrator
                           │                     │
                     (bounded thread pool,       ├──▶ CrawlSkill      (Crawlability & rendering)
                      hard timeout)              ├──▶ SchemaSkill     (JSON-LD & metadata)
                           │                     ├──▶ FreshnessSkill  (Stale/consistent facts)
                           ▼                     └──▶ EngagementSkill (UX & content clarity)
                     ReportGenerator
                           │
                           ▼
                     AuditReport (JSON)
                           │
                           ▼
                  Rendered Dashboard (Browser)
```

Every skill runs against **one shared crawl** of the page (`HttpFetcher` fetches it exactly once) — no skill re-fetches the target site.

---

## Audit Skills & Evaluation Criteria

| Skill Module | Identifier | What It Audits |
| :--- | :--- | :--- |
| **`CrawlSkill`** | `crawl-render-audit` | HTTP status codes, robots meta directives, canonical tags, raw HTML word-count heuristics, and AI crawler accessibility. |
| **`SchemaSkill`** | `schema-audit` | Presence and validity of `application/ld+json` structured data, OpenGraph tags, title length, and meta descriptions. |
| **`FreshnessSkill`** | `freshness-audit` | Publication/modification timestamps, temporal metadata consistency, and outdated year/fact signals. |
| **`EngagementSkill`** | `engagement-audit` | Heading hierarchy (`H1`–`H6`), semantic HTML structure, readability, call-to-action clarity, and link/image accessibility. |

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3 (Spring MVC, Embedded Tomcat) |
| **Build Tool** | Apache Maven |
| **Frontend** | Plain HTML5, CSS3, Vanilla JavaScript (zero framework, no build step) |
| **HTML Parsing** | Jsoup |
| **JSON Serialization** | Jackson (via Spring Boot starter web) |
| **Validation** | Jakarta Bean Validation |
| **Monitoring & Health** | Spring Boot Actuator (`/actuator/health`) |
| **Testing** | JUnit 5, Mockito, Spring `MockMvc` |
| **Containerization** | Docker & Docker Compose (Multi-stage build, non-root JRE runtime) |

---

## Project Structure

```text
ai-readiness-webapp/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── src/main/java/com/readinessaudit/webapp/
│   ├── AiReadinessWebApplication.java   # Spring Boot entrypoint
│   ├── controller/
│   │   └── AuditController.java         # REST API: POST /api/audit, GET /api/ping
│   ├── service/
│   │   └── AuditService.java            # Timeout enforcement & exception translation
│   ├── config/
│   │   ├── AsyncConfig.java             # Bounded thread pool for audits
│   │   └── RateLimitFilter.java         # Per-IP rate limiting on /api/audit
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # Consistent JSON error responses
│   │   ├── InvalidUrlException.java
│   │   ├── AuditTimeoutException.java
│   │   └── AuditFailedException.java
│   ├── dto/
│   │   ├── AuditRequest.java            # Validated request body
│   │   └── ErrorResponse.java           # Standardized error JSON shape
│   ├── crawler/
│   │   ├── HttpFetcher.java             # Fetches + parses a page (Jsoup)
│   │   └── CrawlResult.java             # Shared crawl data model
│   ├── skills/
│   │   ├── Skill.java                   # Common audit skill interface
│   │   ├── CrawlSkill.java              # Crawlability & rendering checks
│   │   ├── SchemaSkill.java             # JSON-LD & metadata checks
│   │   ├── FreshnessSkill.java          # Stale/consistent facts checks
│   │   └── EngagementSkill.java         # UX & content clarity checks
│   ├── orchestrator/
│   │   └── AuditOrchestrator.java       # Runs all skills against one shared crawl
│   └── report/
│       ├── Finding.java                 # Single evidence-backed issue
│       ├── AuditReport.java             # Final JSON response model
│       └── ReportGenerator.java         # Weighted scoring + severity prioritization
├── src/main/resources/
│   ├── application.yml                  # Externalized configuration
│   └── static/
│       ├── index.html                   # Interactive audit dashboard UI
│       ├── css/style.css
│       └── js/app.js
└── src/test/java/com/readinessaudit/webapp/
    ├── report/ReportGeneratorTest.java      # Scoring logic unit tests
    ├── service/AuditServiceTest.java        # Timeout & error handling tests (Mockito)
    └── controller/AuditControllerTest.java  # Validation & API contract tests (MockMvc)
```

---

## Getting Started

### Prerequisites

- **JDK 21** or higher
- **Maven 3.9+** with internet access (to pull Spring Boot, Jsoup, and test dependencies from Maven Central)
- **Docker** (optional, for containerized execution)

### Run Locally with Maven

1. Clone the repository:
   ```bash
   git clone [https://github.com/Utkarsh-1129/WebISight-Website-Readiness-AI-Discoverability.git](https://github.com/Utkarsh-1129/WebISight-Website-Readiness-AI-Discoverability.git)
   cd WebISight-Website-Readiness-AI-Discoverability
   ```

2. Start the Spring Boot server:
   ```bash
   mvn spring-boot:run
   ```

3. Open **http://localhost:8080** in your browser and enter any website URL.

Alternatively, build and execute the packaged JAR directly:

```bash
mvn clean package
java -jar target/ai-readiness-webapp.jar
```

### Run with Docker Compose

```bash
docker compose up --build
```

This compiles the application inside a Maven build stage, packages the artifact into a slim JRE runtime image running as an unprivileged non-root user, exposes port `8080`, and configures a container healthcheck against `/actuator/health`.

---

## Environment Configuration

All operational parameters in `src/main/resources/application.yml` are externalized and can be overridden via environment variables without rebuilding the image:

| Environment Variable | Description | Default |
| :--- | :--- | :--- |
| `SERVER_PORT` | HTTP port exposed by the embedded Tomcat server | `8080` |
| `AUDIT_TIMEOUT_SECONDS` | Hard timeout in seconds for a single website audit | `15` |
| `AUDIT_EXECUTOR_POOL_SIZE` | Maximum worker threads in the bounded audit thread pool | `10` |
| `AUDIT_RATE_LIMIT_PER_MINUTE` | Maximum `/api/audit` requests permitted per IP per minute | `20` |

---

## REST API Reference

### 1. Execute Website Audit

`POST /api/audit`

**Request Headers:**
```http
Content-Type: application/json
```

**Request Body:**
```json
{
  "url": "[https://example.com](https://example.com)"
}
```

**Example `curl` Command:**
```bash
curl -X POST http://localhost:8080/api/audit \
  -H "Content-Type: application/json" \
  -d '{"url": "[https://example.com](https://example.com)"}'
```

**Response (`200 OK`):**
```json
{
  "url": "[https://example.com](https://example.com)",
  "generatedAt": "2026-09-26T12:00:00Z",
  "overallScore": 90,
  "skillScores": {
    "crawl-render-audit": 100,
    "schema-audit": 82,
    "freshness-audit": 90,
    "engagement-audit": 90
  },
  "summary": {
    "totalChecks": 9,
    "passed": 6,
    "failed": 3,
    "critical": 0,
    "high": 1,
    "medium": 2,
    "low": 0
  },
  "prioritizedActions": [
    {
      "skill": "schema-audit",
      "severity": "HIGH",
      "issue": "Missing JSON-LD structured data",
      "evidence": "No <script type=\"application/ld+json\"> found on page",
      "recommendation": "Embed valid Schema.org JSON-LD markup to improve entity extraction by AI crawlers."
    }
  ],
  "findingsBySkill": {
    "crawl-render-audit": [],
    "schema-audit": [],
    "freshness-audit": [],
    "engagement-audit": []
  }
}
```

> `prioritizedActions` is automatically ordered worst-severity-first (`CRITICAL` → `HIGH` → `MEDIUM` → `LOW`).

**Error Responses (`400`, `429`, `502`, `504`, `500`):**

All error paths return a consistent JSON schema via `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-09-26T12:00:05Z",
  "status": 400,
  "error": "Bad Request",
  "message": "url must start with http:// or https://"
}
```

| Status Code | Exception / Trigger | Reason |
| :--- | :--- | :--- |
| `400 Bad Request` | `InvalidUrlException` / Bean Validation | Blank URL or missing `http://` / `https://` scheme |
| `429 Too Many Requests` | `RateLimitFilter` | Client IP exceeded `AUDIT_RATE_LIMIT_PER_MINUTE` |
| `502 Bad Gateway` | `AuditFailedException` | Target site unreachable, DNS failure, or invalid HTTP response |
| `504 Gateway Timeout` | `AuditTimeoutException` | Target audit exceeded `AUDIT_TIMEOUT_SECONDS` |
| `500 Internal Server Error` | Unhandled Exception | Unexpected internal processing error |

---

### 2. API Liveness Check

`GET /api/ping`

Lightweight liveness probe verifying that the Spring MVC controller layer is responsive.

---

### 3. Container & Orchestrator Readiness Probe

`GET /actuator/health`

Spring Boot Actuator health endpoint used by Docker Compose healthchecks and cloud load balancers (Railway, Kubernetes, AWS ECS).

---

## Production Engineering Highlights

- **Bounded Thread Pool + Hard Timeout (`AsyncConfig`, `AuditService`):** Slow or hanging target websites can never tie up a request indefinitely or starve Tomcat's request-handling threads.
- **Per-IP Rate Limiting (`RateLimitFilter`):** Applied specifically to `/api/audit` since each invocation performs outbound network I/O. Designed as an in-memory filter for single-instance deployments; multi-instance horizontal deployments behind a load balancer can back this with Redis.
- **Centralized Exception Translation (`GlobalExceptionHandler`):** Raw stack traces never reach the client; every failure mode maps cleanly to a structured `ErrorResponse`.
- **Upfront Input Validation (`AuditRequest`):** Rejects malformed or non-HTTP(S) URLs before opening any network connection.
- **Structured Logging:** Logs audit start, duration in milliseconds, and final score or failure context.
- **Non-Root Container Hardening:** The production Docker image runs the JVM under an unprivileged system user.

---

## Testing

Execute the automated test suite with Maven:

```bash
mvn test
```

- **`ReportGeneratorTest`:** Verifies weighted scoring math and severity prioritization ordering against synthetic findings without network dependencies.
- **`AuditServiceTest`:** Verifies timeout enforcement and exception translation using Mockito, including real timeout triggers under a tight time budget.
- **`AuditControllerTest`:** Verifies request payload validation, HTTP status codes, and JSON API contracts using Spring `MockMvc`.

---

## Extending with a New Audit Skill

The auditing engine uses Spring's dependency injection for zero-config extensibility:

1. Create a new class implementing `com.readinessaudit.webapp.skills.Skill` and annotate it with `@Component`:

   ```java
   package com.readinessaudit.webapp.skills;

   import com.readinessaudit.webapp.crawler.CrawlResult;
   import com.readinessaudit.webapp.report.Finding;
   import org.springframework.stereotype.Component;
   import java.util.List;

   @Component
   public class SecurityHeadersSkill implements Skill {

       @Override
       public String getName() {
           return "security-headers-audit";
       }

       @Override
       public List<Finding> evaluate(CrawlResult crawl) {
           // Inspect crawl data and return a list of Finding objects
           return List.of();
       }
   }
   ```

2. **That's it.** Spring automatically discovers the new `Skill` bean and injects it into `AuditOrchestrator`. No modifications to `AuditOrchestrator`, `AuditService`, or `AuditController` are required.

---

## Scope & Design Trade-offs

- **Static HTML Inspection vs. Headless Browser JS Rendering:** `CrawlSkill` inspects raw HTML and word-count heuristics rather than running a headless browser (such as Playwright or Puppeteer). Because most AI crawlers and LLM retrieval agents do not execute client-side JavaScript either, static HTML evaluation accurately reflects real-world AI discoverability while keeping container memory usage minimal.
- **Single-URL Auditing:** The API audits one URL per request by design. Whole-site sitemap crawling requires asynchronous job queues, politeness delays, and loop detection, which are intentionally outside the scope of an interactive real-time analyzer.
- **Stateless Execution:** Every request is stateless and self-contained without requiring an external database.

---

## Author

**Utkarsh Trivedi**  
- **GitHub:** [@Utkarsh-1129](https://github.com/Utkarsh-1129)
- **LinkedIn:** [linkedin.com/in/utkarsh1129](https://www.linkedin.com/in/utkarsh1129)
- **Email:** [utkarshtrivedi12d@gmail.com](mailto:utkarshtrivedi12d@gmail.com)
