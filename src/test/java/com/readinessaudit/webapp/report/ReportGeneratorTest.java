package com.readinessaudit.webapp.report;

import com.readinessaudit.webapp.crawler.CrawlResult;
import com.readinessaudit.webapp.orchestrator.AuditOrchestrator.AuditRunResult;
import com.readinessaudit.webapp.report.Finding.Severity;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for scoring + prioritization logic, using synthetic findings
 * so no network access is required.
 */
class ReportGeneratorTest {

    private CrawlResult dummyCrawlResult() {
        return new CrawlResult(
                "https://test.example",
                200, 100, "<html></html>",
                "Test Title", "A description",
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), Collections.emptyList(),
                true, true, "",
                Collections.emptyList(), 0, 0, 500
        );
    }

    @Test
    void allPassingChecksYieldsMaxScore() {
        Map<String, List<Finding>> findings = new LinkedHashMap<>();
        findings.put("crawl-render-audit", List.of(
                Finding.builder().skill("crawl-render-audit").checkName("http_status")
                        .severity(Severity.INFO).passed(true).issue("ok").build()
        ));

        AuditRunResult runResult = new AuditRunResult(dummyCrawlResult(), findings);
        AuditReport report = new ReportGenerator().generate(runResult);

        assertEquals(100, report.overallScore);
        assertEquals(0, report.summary.failed);
        assertTrue(report.prioritizedActions.isEmpty());
    }

    @Test
    void criticalFailureLowersScoreAndTopsPriorityList() {
        Map<String, List<Finding>> findings = new LinkedHashMap<>();
        findings.put("crawl-render-audit", List.of(
                Finding.builder().skill("crawl-render-audit").checkName("http_status")
                        .severity(Severity.CRITICAL).passed(false).issue("bad status").build(),
                Finding.builder().skill("crawl-render-audit").checkName("title_tag")
                        .severity(Severity.LOW).passed(false).issue("long title").build()
        ));

        AuditRunResult runResult = new AuditRunResult(dummyCrawlResult(), findings);
        AuditReport report = new ReportGenerator().generate(runResult);

        assertTrue(report.overallScore < 100);
        assertEquals(1, report.summary.critical);
        assertEquals(Severity.CRITICAL, report.prioritizedActions.get(0).getSeverity());
    }

    @Test
    void passedFindingsAreExcludedFromPrioritizedActions() {
        Map<String, List<Finding>> findings = new LinkedHashMap<>();
        findings.put("engagement-audit", List.of(
                Finding.builder().skill("engagement-audit").checkName("h1_structure")
                        .severity(Severity.INFO).passed(true).issue("fine").build(),
                Finding.builder().skill("engagement-audit").checkName("content_depth")
                        .severity(Severity.MEDIUM).passed(false).issue("thin content").build()
        ));

        AuditRunResult runResult = new AuditRunResult(dummyCrawlResult(), findings);
        AuditReport report = new ReportGenerator().generate(runResult);

        assertEquals(1, report.prioritizedActions.size());
        assertEquals("content_depth", report.prioritizedActions.get(0).getCheckName());
    }
}
