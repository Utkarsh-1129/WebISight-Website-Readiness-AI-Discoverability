(function () {
  "use strict";

  const form = document.getElementById("audit-form");
  const urlInput = document.getElementById("url-input");
  const submitBtn = document.getElementById("submit-btn");
  const formError = document.getElementById("form-error");
  const loading = document.getElementById("loading");
  const results = document.getElementById("results");

  const overallRing = document.getElementById("overall-ring");
  const overallScoreValue = document.getElementById("overall-score-value");
  const auditedUrl = document.getElementById("audited-url");
  const skillScoresEl = document.getElementById("skill-scores");
  const summaryBar = document.getElementById("summary-bar");
  const prioritizedList = document.getElementById("prioritized-list");
  const noIssues = document.getElementById("no-issues");
  const allFindingsEl = document.getElementById("all-findings");
  const downloadBtn = document.getElementById("download-json");

  let lastReport = null;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    hideError();

    const url = urlInput.value.trim();
    if (!/^https?:\/\/.+/i.test(url)) {
      showError("Enter a full URL starting with http:// or https://");
      return;
    }

    setLoading(true);
    results.hidden = true;

    try {
      const response = await fetch("/api/audit", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url }),
      });

      const body = await response.json();

      if (!response.ok) {
        showError(body.message || "The audit could not be completed.");
        return;
      }

      lastReport = body;
      renderReport(body);
      results.hidden = false;
    } catch (err) {
      showError("Could not reach the audit service. Check your connection and try again.");
    } finally {
      setLoading(false);
    }
  });

  downloadBtn.addEventListener("click", () => {
    if (!lastReport) return;
    const blob = new Blob([JSON.stringify(lastReport, null, 2)], { type: "application/json" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "audit-report.json";
    link.click();
    URL.revokeObjectURL(link.href);
  });

  function setLoading(isLoading) {
    loading.hidden = !isLoading;
    submitBtn.disabled = isLoading;
  }

  function showError(message) {
    formError.textContent = message;
    formError.hidden = false;
  }

  function hideError() {
    formError.hidden = true;
  }

  function scoreColor(score) {
    if (score >= 85) return "#34d399";
    if (score >= 60) return "#fbbf24";
    return "#f87171";
  }

  function renderReport(report) {
    // Overall score ring
    overallRing.style.setProperty("--pct", report.overallScore);
    overallRing.style.background =
      `conic-gradient(${scoreColor(report.overallScore)} calc(${report.overallScore} * 1%), var(--border) 0)`;
    overallScoreValue.textContent = report.overallScore;
    auditedUrl.textContent = report.url;

    // Per-skill score cards
    skillScoresEl.innerHTML = "";
    Object.entries(report.skillScores).forEach(([skill, score]) => {
      const card = document.createElement("div");
      card.className = "skill-score-card";
      card.innerHTML = `
        <div class="name">${formatSkillName(skill)}</div>
        <div class="value" style="color:${scoreColor(score)}">${score}</div>
      `;
      skillScoresEl.appendChild(card);
    });

    // Summary chips
    const s = report.summary;
    summaryBar.innerHTML = "";
    const chips = [
      { label: `${s.passed} passed`, color: "#34d399" },
      { label: `${s.critical} critical`, color: "#f87171" },
      { label: `${s.high} high`, color: "#fb923c" },
      { label: `${s.medium} medium`, color: "#fbbf24" },
      { label: `${s.low} low`, color: "#60a5fa" },
    ];
    chips.forEach((chip) => {
      const el = document.createElement("span");
      el.className = "summary-chip";
      el.style.color = chip.color;
      el.textContent = chip.label;
      summaryBar.appendChild(el);
    });

    // Prioritized fix list
    prioritizedList.innerHTML = "";
    if (report.prioritizedActions.length === 0) {
      noIssues.hidden = false;
    } else {
      noIssues.hidden = true;
      report.prioritizedActions.forEach((f) => {
        prioritizedList.appendChild(renderFinding(f));
      });
    }

    // All findings grouped by skill
    allFindingsEl.innerHTML = "";
    Object.entries(report.findingsBySkill).forEach(([skill, findings]) => {
      const group = document.createElement("div");
      group.className = "skill-group";
      const heading = document.createElement("h3");
      heading.textContent = formatSkillName(skill);
      group.appendChild(heading);

      const list = document.createElement("div");
      list.className = "finding-list";
      findings.forEach((f) => list.appendChild(renderFinding(f)));
      group.appendChild(list);

      allFindingsEl.appendChild(group);
    });
  }

  function renderFinding(f) {
    const el = document.createElement("div");
    el.className = `finding sev-${f.severity}` + (f.passed ? " passed" : "");
    el.innerHTML = `
      <div class="finding-top">
        <span class="badge sev-${f.severity}">${f.severity}</span>
        <span class="finding-skill">${formatSkillName(f.skill)} · ${f.checkName}</span>
      </div>
      <div class="finding-issue">${escapeHtml(f.issue)}</div>
      <div class="finding-evidence">${escapeHtml(f.evidence)}</div>
      <div class="finding-recommendation">${escapeHtml(f.recommendation)}</div>
    `;
    return el;
  }

  function formatSkillName(skill) {
    return skill
      .split("-")
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(" ");
  }

  function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str == null ? "" : String(str);
    return div.innerHTML;
  }
})();
