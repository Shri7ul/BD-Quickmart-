package com.bdquickmart.modules.rider;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.order.OrderService;
import com.bdquickmart.modules.order.dto.OrderResponse;
import com.bdquickmart.modules.order.dto.StatusUpdateRequest;
import com.bdquickmart.modules.rider.dto.AvailabilityRequest;
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

/** Rider delivery workflow. Every state change goes through the backend. */
@RestController
@RequestMapping("/api/v1/rider")
@Tag(name = "Rider delivery", description = "Availability, assignments and delivery workflow")
public class RiderController {

    private final RiderService riderService;
    private final OrderService orderService;

    public RiderController(RiderService riderService, OrderService orderService) {
        this.riderService = riderService;
        this.orderService = orderService;
    }

    @PostMapping("/availability")
    public ApiResponse<Void> availability(
            @Valid @RequestBody AvailabilityRequest body, HttpServletRequest request) {
        riderService.setAvailability(currentPublicId(request), body.online());
        return ApiResponses.success(null, body.online() ? "You are online" : "You are offline", request);
    }

    @GetMapping("/orders/assigned")
    public ApiResponse<List<OrderResponse>> assigned(HttpServletRequest request) {
        return ApiResponses.success(orderService.listForRider(currentPublicId(request)), request);
    }

    @GetMapping("/orders/{publicId}")
    public ApiResponse<OrderResponse> order(@PathVariable String publicId,
                                             HttpServletRequest request) {
        OrderResponse order = orderService.getOrder(publicId, currentPublicId(request), true);
        requireAssignedToMe(order, currentPublicId(request));
        return ApiResponses.success(order, request);
    }

    @PostMapping("/orders/{publicId}/status")
    public ApiResponse<OrderResponse> status(
            @PathVariable String publicId, @Valid @RequestBody StatusUpdateRequest body,
            HttpServletRequest request) {
        requireAssignedToMe(
                orderService.getOrder(publicId, currentPublicId(request), true),
                currentPublicId(request));
        OrderResponse order = orderService.transition(publicId, body, true);
        return ApiResponses.success(order, request);
    }

    private void requireAssignedToMe(OrderResponse order, String riderPublicId) {
        if (order.riderPublicId() == null || !order.riderPublicId().equals(riderPublicId)) {
            throw new com.bdquickmart.common.exception.BusinessException(
                    com.bdquickmart.common.api.ApiCode.FORBIDDEN,
                    "This delivery is not assigned to you");
        }
    }

    private String currentPublicId(HttpServletRequest request) {
        return request.getUserPrincipal() == null ? "" : request.getUserPrincipal().getName();
    }
}
