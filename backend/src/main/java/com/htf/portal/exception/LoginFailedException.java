package com.htf.portal.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the email or password is wrong (HTTP 401). It always has the same message,
 * so an attacker cannot find out which emails have accounts.
 */
public class LoginFailedException extends PortalException {

    public LoginFailedException() {
        super("Invalid email or password", HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS");
    }
}
