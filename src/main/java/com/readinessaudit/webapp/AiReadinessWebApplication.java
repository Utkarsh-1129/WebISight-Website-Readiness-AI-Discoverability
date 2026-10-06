package com.readinessaudit.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AI Readiness Audit web application.
 *
 * Starts an embedded Tomcat server serving:
 *   - the static single-page frontend at /
 *   - the REST API at /api/audit
 *   - health/metrics at /actuator/health
 */
@SpringBootApplication
public class AiReadinessWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiReadinessWebApplication.class, args);
    }
}
