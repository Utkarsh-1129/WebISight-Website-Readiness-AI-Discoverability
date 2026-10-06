package com.readinessaudit.webapp.report;

/**
 * One evidence-backed issue (or pass) discovered by a skill.
 * This is the atomic unit that ends up in the final JSON report.
 */
public class Finding {

    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }

    private final String skill;
    private final String checkName;
    private final Severity severity;
    private final String issue;
    private final String evidence;
    private final String recommendation;
    private final boolean passed;

    private Finding(Builder b) {
        this.skill = b.skill;
        this.checkName = b.checkName;
        this.severity = b.severity;
        this.issue = b.issue;
        this.evidence = b.evidence;
        this.recommendation = b.recommendation;
        this.passed = b.passed;
    }

    public static Builder builder() { return new Builder(); }

    public String getSkill() { return skill; }
    public String getCheckName() { return checkName; }
    public Severity getSeverity() { return severity; }
    public String getIssue() { return issue; }
    public String getEvidence() { return evidence; }
    public String getRecommendation() { return recommendation; }
    public boolean isPassed() { return passed; }

    public static class Builder {
        private String skill;
        private String checkName;
        private Severity severity = Severity.INFO;
        private String issue = "";
        private String evidence = "";
        private String recommendation = "";
        private boolean passed = false;

        public Builder skill(String v) { this.skill = v; return this; }
        public Builder checkName(String v) { this.checkName = v; return this; }
        public Builder severity(Severity v) { this.severity = v; return this; }
        public Builder issue(String v) { this.issue = v; return this; }
        public Builder evidence(String v) { this.evidence = v; return this; }
        public Builder recommendation(String v) { this.recommendation = v; return this; }
        public Builder passed(boolean v) { this.passed = v; return this; }

        public Finding build() { return new Finding(this); }
    }
}
