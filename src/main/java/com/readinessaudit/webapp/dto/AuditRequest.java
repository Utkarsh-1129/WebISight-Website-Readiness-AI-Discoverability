package com.readinessaudit.webapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for POST /api/audit.
 */
public class AuditRequest {

    @NotBlank(message = "url must not be blank")
    @Pattern(
            regexp = "^https?://.+",
            message = "url must start with http:// or https://"
    )
    private String url;

    public AuditRequest() {}

    public AuditRequest(String url) {
        this.url = url;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
