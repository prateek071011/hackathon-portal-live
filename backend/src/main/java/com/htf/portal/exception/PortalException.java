package com.htf.portal.exception;

import org.springframework.http.HttpStatus;

/**
 * Parent of all our business exceptions. It is abstract (you always throw a specific child)
 * and unchecked, so services can throw it without "throws" clauses and @Transactional rolls back.
 * Each child decides which HTTP status and error code the client receives.
 */
public abstract class PortalException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected PortalException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
