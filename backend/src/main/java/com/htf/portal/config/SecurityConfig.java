package com.htf.portal.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.htf.portal.security.JwtAuthFilter;
import com.htf.portal.security.JwtService;
import com.htf.portal.security.SecurityErrorHandler;

/**
 * All security settings in one place: which URLs need which role, JWT instead of sessions,
 * BCrypt for passwords, and CORS (only the React dev server may call the API).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final SecurityErrorHandler securityErrorHandler;
    private final String allowedOrigin;

    public SecurityConfig(JwtService jwtService, SecurityErrorHandler securityErrorHandler,
                          @Value("${portal.cors.allowed-origin}") String allowedOrigin) {
        this.jwtService = jwtService;
        this.securityErrorHandler = securityErrorHandler;
        this.allowedOrigin = allowedOrigin;
    }

    // Spring Security 7 only accepts its settings as small lambdas ("customizers"),
    // so this method uses them even though the rest of the project prefers plain code.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF attacks abuse cookies the browser sends automatically. We use no cookies:
                // the JWT is sent by our own code in a header, so CSRF protection is not needed.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // No server-side session: every request proves who it is with its JWT.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/health", "/error").permitAll()
                        .requestMatchers("/api/college/**").hasRole("COLLEGE")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                // Our filter is created here with "new" (not as a @Component) so Spring Boot does not
                // also register it a second time as a normal servlet filter.
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt adds a random salt and is slow on purpose, which makes stolen hashes hard to crack.
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(allowedOrigin));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Lets the browser read the file name on CSV downloads.
        cors.setExposedHeaders(List.of("Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
