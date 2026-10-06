package com.readinessaudit.webapp.report;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Top-level shape of the audit report returned by POST /api/audit.
 * Spring's built-in Jackson integration serializes this directly to JSON.
 */
public class AuditReport {

    public String url;
    public String generatedAt;
    public int overallScore;                 // 0-100
    public Map<String, Integer> skillScores;  // per-skill score 0-100
    public Summary summary;
    public List<Finding> prioritizedActions;  // sorted worst-severity-first, failures only
    public Map<String, List<Finding>> findingsBySkill;

    public AuditReport() {
        this.generatedAt = Instant.now().toString();
    }

    public static class Summary {
        public int totalChecks;
        public int passed;
        public int failed;
        public int critical;
        public int high;
        public int medium;
        public int low;
    }
}
