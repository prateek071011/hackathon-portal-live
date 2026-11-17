package com.htf.portal.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is well-formed but breaks one of our rules (HTTP 400),
 * e.g. "Need 3 members — you have 2" or "Team is locked after submission".
 */
public class BusinessRuleException extends PortalException {

    public BusinessRuleException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "RULE_VIOLATION");
    }

    // Only subclasses use this one, so they can pass their own error code.
    protected BusinessRuleException(String message, String code) {
        super(message, HttpStatus.BAD_REQUEST, code);
    }
}
