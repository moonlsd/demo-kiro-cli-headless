package com.example.demo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.security.AppUserDetails;
import com.example.demo.security.ProblemDetailAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP security for the application (KIRODEMO-003).
 *
 * <p>The order-history route under {@code /api/v1/**} requires authentication;
 * unauthenticated callers receive a {@code 401} problem-detail (no login redirect)
 * before any controller logic runs (FR-3). The greeting endpoints and the Actuator
 * health endpoint remain public, matching the existing reference surface.
 *
 * <p>Authentication uses stateless HTTP Basic against an in-memory user store. Each
 * user carries the numeric {@code userId} used to scope order queries. In a real
 * deployment this store is replaced by the organization's identity provider; the
 * principal contract ({@link AppUserDetails#getUserId()}) stays the same.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/hello").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").authenticated()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint));
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new ProblemDetailAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * In-memory users carrying their numeric {@code userId}. Replaced by a real
     * identity provider in production; credentials here are for local/demo use only
     * and are not secrets to protect.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        List<AppUserDetails> users = List.of(
                new AppUserDetails(1L, "alice", passwordEncoder.encode("password")),
                new AppUserDetails(2L, "bob", passwordEncoder.encode("password")));

        return username -> users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .map(u -> (org.springframework.security.core.userdetails.UserDetails) u)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
