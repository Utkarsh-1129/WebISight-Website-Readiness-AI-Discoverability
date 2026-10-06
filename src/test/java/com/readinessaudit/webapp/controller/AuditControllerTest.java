package com.readinessaudit.webapp.controller;

import com.readinessaudit.webapp.report.AuditReport;
import com.readinessaudit.webapp.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    void rejectsBlankUrl() throws Exception {
        mockMvc.perform(post("/api/audit")
                        .contentType("application/json")
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsUrlWithoutHttpScheme() throws Exception {
        mockMvc.perform(post("/api/audit")
                        .contentType("application/json")
                        .content("{\"url\":\"ftp://example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsReportForValidUrl() throws Exception {
        AuditReport report = new AuditReport();
        report.url = "https://example.com";
        report.overallScore = 87;
        report.skillScores = Collections.emptyMap();
        report.prioritizedActions = Collections.emptyList();
        report.findingsBySkill = Collections.emptyMap();
        report.summary = new AuditReport.Summary();

        when(auditService.runAudit(anyString())).thenReturn(report);

        mockMvc.perform(post("/api/audit")
                        .contentType("application/json")
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(87))
                .andExpect(jsonPath("$.url").value("https://example.com"));
    }
}
