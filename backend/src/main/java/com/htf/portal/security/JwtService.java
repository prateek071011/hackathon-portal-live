package com.htf.portal.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.htf.portal.model.Role;
import com.htf.portal.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Creates and checks JSON Web Tokens (JWT). After login the browser keeps the token and sends it on
 * every request. The token is signed with our secret key, so if anyone changes it the signature check fails.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_BYTES = 32;   // HS256 needs a key of at least 256 bits

    private final SecretKey key;
    private final long expiryMillis;

    public JwtService(@Value("${portal.jwt.secret}") String secret,
                      @Value("${portal.jwt.expiry-hours}") long expiryHours) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("portal.jwt.secret must be at least " + MIN_SECRET_BYTES + " characters long");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiryMillis = expiryHours * 60 * 60 * 1000;
    }

    public String createToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMillis);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** Returns the user stored in a valid token, or empty if the token is fake, changed or expired. */
    public Optional<AuthUser> readToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            Role role = Role.valueOf(claims.get("role", String.class));
            return Optional.of(new AuthUser(id, email, role));
        } catch (JwtException | IllegalArgumentException e) {
            // IllegalArgumentException covers a missing or unknown role/id inside an otherwise valid token.
            log.warn("Rejected a JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
