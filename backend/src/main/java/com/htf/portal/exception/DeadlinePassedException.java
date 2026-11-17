package com.htf.portal.exception;

import java.time.LocalDate;

/**
 * Thrown when someone tries to create a team, send a request or submit after the
 * hackathon's registration deadline.
 */
public class DeadlinePassedException extends BusinessRuleException {

    public DeadlinePassedException(LocalDate deadline) {
        super("Registration closed on " + deadline, "DEADLINE_PASSED");
    }
}
