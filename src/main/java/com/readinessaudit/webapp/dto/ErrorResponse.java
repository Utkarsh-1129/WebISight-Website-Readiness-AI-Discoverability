package com.readinessaudit.webapp.dto;

import java.time.Instant;

/**
 * Consistent JSON error shape returned by every failure path in the API,
 * handled centrally in GlobalExceptionHandler.
 */
public class ErrorResponse {

    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;

    public ErrorResponse(int status, String error, String message) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
}
