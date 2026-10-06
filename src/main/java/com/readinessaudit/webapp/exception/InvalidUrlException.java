package com.readinessaudit.webapp.exception;

/**
 * Thrown when the requested URL is malformed in a way validation didn't
 * catch (e.g. a syntactically valid but unparseable URI).
 */
public class InvalidUrlException extends RuntimeException {
    public InvalidUrlException(String message) {
        super(message);
    }

    public InvalidUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
