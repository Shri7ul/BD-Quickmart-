package com.bdquickmart.modules.customer;

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
 * Goal 02 authorization probe. It proves the CUSTOMER authority gate works
 * end to end; it contains no Goal 03 business logic.
 */
@RestController("customerRoleProbeController")
@RequestMapping("/api/v1/customer")
@Tag(name = "Customer probe", description = "Goal 02 authorization scaffolding")
public class RoleProbeController {

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "customer area reachable");
        payload.put("role", "CUSTOMER");
        payload.put("publicId", authentication.getName());
        return ResponseEntity.ok(ApiResponses.success(payload, request));
    }
}
