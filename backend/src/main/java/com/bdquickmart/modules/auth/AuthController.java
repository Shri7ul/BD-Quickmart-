package com.bdquickmart.modules.auth;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.auth.dto.AuthResponse;
import com.bdquickmart.modules.auth.dto.LoginRequest;
import com.bdquickmart.modules.auth.dto.RefreshRequest;
import com.bdquickmart.modules.auth.dto.RegisterRequest;
import com.bdquickmart.modules.auth.exception.InvalidRefreshTokenException;
import com.bdquickmart.modules.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public and authenticated authentication endpoints under {@code /api/v1/auth}. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, token refresh and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a CUSTOMER or RIDER account",
            description = "New riders start PENDING and require admin approval.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(user, "Registration successful", httpRequest));
    }

    @Operation(summary = "Log in with email or phone")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponses.success(
                authService.login(request), "Login successful", httpRequest));
    }

    @Operation(summary = "Rotate a refresh token",
            description = "All failures return a single generic 401 to prevent token-state disclosure.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse tokens = authService.refresh(request.refreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);
        return ResponseEntity.ok(ApiResponses.success(tokens, "Token refreshed", httpRequest));
    }

    @Operation(summary = "Log out and revoke all refresh tokens for the current user")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        authService.logout(currentPublicId());
        return ResponseEntity.ok(ApiResponses.<Void>success(null, "Logged out", httpRequest));
    }

    @Operation(summary = "Return the authenticated user's profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponses.success(
                authService.me(currentPublicId()), httpRequest));
    }

    private static String currentPublicId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "" : authentication.getName();
    }
}
