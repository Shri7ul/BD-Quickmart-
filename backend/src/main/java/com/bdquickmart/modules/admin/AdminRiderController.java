package com.bdquickmart.modules.admin;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.auth.AuthService;
import com.bdquickmart.modules.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rider approval workflow, restricted to {@code PERM_APPROVE_RIDERS}. */
@RestController
@RequestMapping("/api/v1/admin/riders")
@Tag(name = "Rider approval", description = "ADMIN / SUPER_ADMIN rider onboarding")
public class AdminRiderController {

    private final AuthService authService;

    public AdminRiderController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> pending(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponses.success(authService.pendingRiders(), request));
    }

    @PostMapping("/{publicId}/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approve(
            @PathVariable String publicId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                authService.approveRider(publicId), "Rider approved", request));
    }

    @PostMapping("/{publicId}/reject")
    public ResponseEntity<ApiResponse<UserResponse>> reject(
            @PathVariable String publicId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                authService.rejectRider(publicId), "Rider rejected", request));
    }
}
