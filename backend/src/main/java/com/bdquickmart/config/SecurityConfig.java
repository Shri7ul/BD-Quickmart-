package com.bdquickmart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security configuration.
 *
 * <p>Goal 01 only establishes the wiring. Endpoint-level authorization,
 * JWT issuance and role management are introduced in Goal 02.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public infrastructure endpoints.
                        .requestMatchers(
                                "/api/v1/health",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**")
                        .permitAll()
                        // The WebSocket handshake is a plain HTTP upgrade request and
                        // never carries a bearer token, so it must bypass the
                        // authenticated filter. Per-connection authorization is
                        // applied by the tracking handler from Goal 08 onwards.
                        .requestMatchers("/ws/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Password hashing for the auth module. BCrypt defaults are intentionally
     * left at strength 10.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
