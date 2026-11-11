package com.bdquickmart.config;

import com.bdquickmart.modules.auth.JwtAuthenticationFilter;
import com.bdquickmart.modules.auth.RestAccessDeniedHandler;
import com.bdquickmart.modules.auth.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Stateless JWT security configuration for Goal 02.
 *
 * <p>Authorization is expressed with literal authorities (no
 * {@code RoleHierarchy} and no {@code @PreAuthorize}), so every 401/403 flows
 * through one consistent JSON path. CORS is supplied by a single
 * {@link CorsConfigurationSource} bean, which also covers filter-level error
 * responses.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Preflight and public infrastructure.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/v1/health",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/ws/**")
                        .permitAll()
                        // Public auth endpoints.
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh")
                        .permitAll()
                        // Authenticated auth endpoints.
                        .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout")
                        .authenticated()
                        // Role probes (Goal 02 authorization scaffolding).
                        .requestMatchers("/api/v1/customer/**")
                        .hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers("/api/v1/rider/**")
                        .hasAuthority("PERM_RIDER_ACTIVE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/ping")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN",
                                "ROLE_STORE_MANAGER", "ROLE_INVENTORY_OPERATOR")
                        // Rider approval (ADMIN + SUPER_ADMIN).
                        .requestMatchers("/api/v1/admin/riders/**")
                        .hasAuthority("PERM_APPROVE_RIDERS")
                        // Privileged provisioning (SUPER_ADMIN only).
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/users/provision")
                        .hasAuthority("PERM_PROVISION_PRIVILEGED")
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt strength 10 is intentionally left at the Spring default. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
