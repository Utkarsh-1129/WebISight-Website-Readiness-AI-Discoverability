package com.readinessaudit.webapp.report;

import com.readinessaudit.webapp.orchestrator.AuditOrchestrator.AuditRunResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a raw AuditRunResult (crawl + per-skill findings) into the final
 * AuditReport: computes scores, sorts findings into a prioritized action
 * list (worst severity first).
 */
@Component
public class ReportGenerator {

    public AuditReport generate(AuditRunResult runResult) {
        AuditReport report = new AuditReport();
        report.url = runResult.getCrawlResult().getUrl();
        report.findingsBySkill = runResult.getFindingsBySkill();

        List<Finding> allFindings = runResult.getAllFindings();

        report.summary = buildSummary(allFindings);
        report.skillScores = computeSkillScores(runResult.getFindingsBySkill());
        report.overallScore = computeOverallScore(report.skillScores);
        report.prioritizedActions = prioritize(allFindings);

        return report;
    }

    private AuditReport.Summary buildSummary(List<Finding> findings) {
        AuditReport.Summary summary = new AuditReport.Summary();
        summary.totalChecks = findings.size();
        for (Finding f : findings) {
            if (f.isPassed()) {
                summary.passed++;
            } else {
                summary.failed++;
                switch (f.getSeverity()) {
                    case CRITICAL -> summary.critical++;
                    case HIGH -> summary.high++;
                    case MEDIUM -> summary.medium++;
                    case LOW -> summary.low++;
                    default -> { /* INFO doesn't count as a failure */ }
                }
            }
        }
        return summary;
    }

    /** Deduction-based scoring per skill: start at 100, subtract weighted penalties. */
    private Map<String, Integer> computeSkillScores(Map<String, List<Finding>> findingsBySkill) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Map.Entry<String, List<Finding>> entry : findingsBySkill.entrySet()) {
            int score = 100;
            for (Finding f : entry.getValue()) {
                if (f.isPassed()) continue;
                score -= severityPenalty(f.getSeverity());
            }
            scores.put(entry.getKey(), Math.max(score, 0));
        }
        return scores;
    }

    private int computeOverallScore(Map<String, Integer> skillScores) {
        if (skillScores.isEmpty()) return 0;
        int sum = skillScores.values().stream().mapToInt(Integer::intValue).sum();
        return sum / skillScores.size();
    }

    private int severityPenalty(Finding.Severity severity) {
        return switch (severity) {
            case CRITICAL -> 30;
            case HIGH -> 18;
            case MEDIUM -> 10;
            case LOW -> 4;
            case INFO -> 0;
        };
    }

    /** Worst severity first, so the top of the list is what to fix first. */
    private List<Finding> prioritize(List<Finding> findings) {
        List<Finding> failuresOnly = new ArrayList<>();
        for (Finding f : findings) {
            if (!f.isPassed()) failuresOnly.add(f);
        }
        failuresOnly.sort(Comparator.comparingInt(f -> severityRank(f.getSeverity())));
        return failuresOnly;
    }

    private int severityRank(Finding.Severity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
            case INFO -> 4;
        };
    }
}
