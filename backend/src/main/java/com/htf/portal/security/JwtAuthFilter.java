package com.htf.portal.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Our one security filter. It runs once per request, before any controller: if the request has
 * "Authorization: Bearer <token>" and the token is valid, it tells Spring Security who the user is.
 * It never blocks a request itself; the rules in SecurityConfig decide that.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            Optional<AuthUser> authUser = jwtService.readToken(token);
            if (authUser.isPresent()) {
                AuthUser user = authUser.get();
                // Spring Security's hasRole("ADMIN") looks for the authority "ROLE_ADMIN".
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        // Always continue. Without a valid token the request is simply anonymous.
        chain.doFilter(request, response);
    }
}
