package com.bdquickmart.modules.auth;

import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the bearer access token on each request and populates the
 * {@link SecurityContextHolder}.
 *
 * <p>Authorities are always rebuilt from the <em>database</em> row, not from
 * the token claims: a role or status change therefore takes effect on the very
 * next request. Rider status is re-read here so only ACTIVE riders receive
 * {@code PERM_RIDER_ACTIVE}. On any token problem the context is cleared and
 * a request attribute records the specific reason for the entry point.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute consulted by {@link RestAuthenticationEntryPoint}. */
    public static final String AUTH_ERROR = "bdqm.auth.error";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api-docs")
                || path.startsWith("/ws/")
                || path.equals("/api/v1/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.length() <= 7
                || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();

        try {
            TokenClaims claims = jwtService.parse(token);
            if (!claims.isAccess()) {
                request.setAttribute(AUTH_ERROR, ApiCode.TOKEN_INVALID.getCode());
                SecurityContextHolder.clearContext();
            } else {
                UserEntity user = userRepository.findByPublicId(claims.subject()).orElse(null);
                if (user == null || user.getDeletedAt() != null
                        || user.getStatus() == AccountStatus.DISABLED
                        || user.getStatus() == AccountStatus.REJECTED) {
                    request.setAttribute(AUTH_ERROR, ApiCode.UNAUTHORIZED.getCode());
                    SecurityContextHolder.clearContext();
                } else {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user.getPublicId(), null, buildAuthorities(user));
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR, ApiCode.TOKEN_EXPIRED.getCode());
            SecurityContextHolder.clearContext();
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(AUTH_ERROR, ApiCode.TOKEN_INVALID.getCode());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> buildAuthorities(UserEntity user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        switch (user.getRole()) {
            case RIDER -> {
                if (user.getStatus() == AccountStatus.ACTIVE) {
                    authorities.add(new SimpleGrantedAuthority("PERM_RIDER_ACTIVE"));
                }
            }
            case ADMIN -> authorities.add(new SimpleGrantedAuthority("PERM_APPROVE_RIDERS"));
            case SUPER_ADMIN -> {
                authorities.add(new SimpleGrantedAuthority("PERM_APPROVE_RIDERS"));
                authorities.add(new SimpleGrantedAuthority("PERM_PROVISION_PRIVILEGED"));
            }
            default -> {
                // CUSTOMER, STORE_MANAGER and INVENTORY_OPERATOR get only ROLE_*.
            }
        }
        return authorities;
    }
}
