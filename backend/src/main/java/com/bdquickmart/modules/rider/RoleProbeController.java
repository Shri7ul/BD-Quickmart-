package com.bdquickmart.modules.rider;

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
 * Goal 02 authorization probe. Reaching this endpoint requires
 * {@code PERM_RIDER_ACTIVE}, which is only granted to ACTIVE riders.
 */
@RestController("riderRoleProbeController")
@RequestMapping("/api/v1/rider")
@Tag(name = "Rider probe", description = "Goal 02 authorization scaffolding")
public class RoleProbeController {

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "rider area reachable");
        payload.put("role", "RIDER");
        payload.put("publicId", authentication.getName());
        return ResponseEntity.ok(ApiResponses.success(payload, request));
    }
}
