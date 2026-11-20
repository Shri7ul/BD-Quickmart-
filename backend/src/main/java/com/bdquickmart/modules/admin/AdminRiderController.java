package com.bdquickmart.modules.admin;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.auth.AuthService;
import com.bdquickmart.modules.rider.RiderService;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRepository;
import com.bdquickmart.modules.user.UserRole;
import com.bdquickmart.modules.admin.dto.RiderEligibilityResponse;
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
    private final UserRepository userRepository;
    private final RiderService riderService;

    public AdminRiderController(AuthService authService, UserRepository userRepository,
                                RiderService riderService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.riderService = riderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RiderEligibilityResponse>>> all(HttpServletRequest request) {
        List<RiderEligibilityResponse> riders = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.RIDER)
                .map(u -> new RiderEligibilityResponse(u.getPublicId(), u.getFullName(), u.getPhone(),
                        u.getStatus().name(), riderService.isOnline(u.getId())))
                .toList();
        return ResponseEntity.ok(ApiResponses.success(riders, request));
    }

    @GetMapping("/eligible")
    public ResponseEntity<ApiResponse<List<RiderEligibilityResponse>>> eligible(HttpServletRequest request) {
        List<RiderEligibilityResponse> riders = riderService.eligibleRiders().stream()
                .filter(u -> u.getStatus() == AccountStatus.ACTIVE)
                .map(u -> new RiderEligibilityResponse(u.getPublicId(), u.getFullName(), u.getPhone(),
                        u.getStatus().name(), riderService.isOnline(u.getId())))
                .toList();
        return ResponseEntity.ok(ApiResponses.success(riders, request));
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
