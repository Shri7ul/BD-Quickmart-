package com.bdquickmart.modules.admin;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.auth.AuthService;
import com.bdquickmart.modules.auth.dto.ProvisionRequest;
import com.bdquickmart.modules.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Privileged account provisioning, restricted to SUPER_ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "User provisioning", description = "SUPER_ADMIN only")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/provision")
    public ResponseEntity<ApiResponse<UserResponse>> provision(
            @Valid @RequestBody ProvisionRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponses.success(
                authService.provision(request), "Account provisioned", httpRequest));
    }
}
