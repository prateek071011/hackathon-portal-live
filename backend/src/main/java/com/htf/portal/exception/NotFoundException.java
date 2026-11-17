package com.htf.portal.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a row we look up does not exist (HTTP 404).
 * Usually comes from dao.findById(id).orElseThrow(...).
 */
public class NotFoundException extends PortalException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    // Overloaded constructor: builds a standard message like "Team 5 not found".
    public NotFoundException(String what, Object id) {
        this(what + " " + id + " not found");
    }
}
