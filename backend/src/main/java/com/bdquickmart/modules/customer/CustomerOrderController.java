package com.bdquickmart.modules.customer;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.order.OrderService;
import com.bdquickmart.modules.order.dto.CheckoutRequest;
import com.bdquickmart.modules.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Customer checkout and order history. Customers can only see their own orders. */
@RestController
@RequestMapping("/api/v1/customer")
@Tag(name = "Customer orders", description = "Checkout and order tracking")
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponses.success(
                orderService.checkout(currentPublicId(request), body), request));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> orders(HttpServletRequest request) {
        return ApiResponses.success(orderService.listForCustomer(currentPublicId(request)), request);
    }

    @GetMapping("/orders/{publicId}")
    public ApiResponse<OrderResponse> order(@PathVariable String publicId,
                                             HttpServletRequest request) {
        return ApiResponses.success(
                orderService.getOrder(publicId, currentPublicId(request), false), request);
    }

    private String currentPublicId(HttpServletRequest request) {
        return request.getUserPrincipal() == null ? "" : request.getUserPrincipal().getName();
    }
}
