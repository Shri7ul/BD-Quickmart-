package com.bdquickmart.modules.admin;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Goal 02 authorization probe reachable by any of the four privileged roles
 * (ADMIN, SUPER_ADMIN, STORE_MANAGER, INVENTORY_OPERATOR).
 */
@RestController("adminRoleProbeController")
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin probe", description = "Goal 02 authorization scaffolding")
public class RoleProbeController {

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "admin area reachable");
        payload.put("publicId", authentication.getName());
        payload.put("authorities", authentication.getAuthorities().stream()
                .map(Object::toString).sorted().toList());
        return ResponseEntity.ok(ApiResponses.success(payload, request));
    }
}
