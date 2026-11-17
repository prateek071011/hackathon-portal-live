package com.htf.portal.security;

import com.htf.portal.model.Role;

/**
 * The logged-in user as read from a valid JWT: just id, email and role.
 * Controllers receive it with @AuthenticationPrincipal and pass the id to services,
 * which load the full User from the database when they need more.
 */
public record AuthUser(Long id, String email, Role role) {
}
