package com.htf.portal.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes our usual error JSON when Spring Security blocks a request before it reaches a controller
 * (so GlobalExceptionHandler never sees it): 401 when nobody is logged in, 403 when the role is wrong.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // Called when the request has no valid token but the URL needs one.
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(response, 401, "UNAUTHORIZED", "Please log in first");
    }

    // Called when the user is logged in but their role may not use this URL.
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, 403, "FORBIDDEN", "Your account type cannot use this page");
    }

    // The messages are fixed text written by us, so building the JSON by hand is safe here.
    private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = "{\"success\":false,\"message\":\"" + message + "\","
                + "\"data\":{\"status\":" + status + ",\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}";
        response.getWriter().write(json);
    }
}
