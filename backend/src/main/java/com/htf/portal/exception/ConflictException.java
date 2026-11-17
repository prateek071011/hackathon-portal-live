package com.htf.portal.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request clashes with data that already exists (HTTP 409),
 * e.g. an email that is already registered or a team name already used in this hackathon.
 * TeamFullException and AlreadyInTeamException are more specific kinds of conflict.
 */
public class ConflictException extends PortalException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    // Only subclasses use this one, so they can pass their own error code.
    protected ConflictException(String message, String code) {
        super(message, HttpStatus.CONFLICT, code);
    }
}
