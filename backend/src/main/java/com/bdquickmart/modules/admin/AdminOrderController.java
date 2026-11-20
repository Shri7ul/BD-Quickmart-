package com.bdquickmart.modules.admin;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.order.OrderService;
import com.bdquickmart.modules.order.dto.AssignRiderRequest;
import com.bdquickmart.modules.order.dto.DashboardSummaryResponse;
import com.bdquickmart.modules.order.dto.OrderResponse;
import com.bdquickmart.modules.order.dto.StatusUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin order operations and live dashboard figures. */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin orders", description = "Order operations and dashboard metrics")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> orders(HttpServletRequest request) {
        return ApiResponses.success(orderService.listAll(), request);
    }

    @GetMapping("/orders/{publicId}")
    public ApiResponse<OrderResponse> order(@PathVariable String publicId,
                                             HttpServletRequest request) {
        return ApiResponses.success(
                orderService.getOrder(publicId, currentPublicId(request), true), request);
    }

    @PostMapping("/orders/{publicId}/status")
    public ApiResponse<OrderResponse> status(
            @PathVariable String publicId, @Valid @RequestBody StatusUpdateRequest body,
            HttpServletRequest request) {
        return ApiResponses.success(orderService.transition(publicId, body, false), request);
    }

    @PostMapping("/orders/{publicId}/assign")
    public ApiResponse<OrderResponse> assign(
            @PathVariable String publicId, @Valid @RequestBody AssignRiderRequest body,
            HttpServletRequest request) {
        return ApiResponses.success(orderService.assignRider(publicId, body), request);
    }

    @GetMapping("/orders/assignments/available")
    public ApiResponse<List<OrderResponse>> availableAssignments(HttpServletRequest request) {
        return ApiResponses.success(orderService.availableAssignments(), request);
    }

    @GetMapping("/dashboard/summary")
    public ApiResponse<DashboardSummaryResponse> dashboard(HttpServletRequest request) {
        return ApiResponses.success(orderService.dashboard(), request);
    }

    private String currentPublicId(HttpServletRequest request) {
        return request.getUserPrincipal() == null ? "" : request.getUserPrincipal().getName();
    }
}
