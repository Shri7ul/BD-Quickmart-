package com.bdquickmart.modules.admin;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.inventory.InventoryService;
import com.bdquickmart.modules.inventory.dto.AdjustmentResponse;
import com.bdquickmart.modules.inventory.dto.InventoryResponse;
import com.bdquickmart.modules.inventory.dto.StockAdjustmentRequest;
import com.bdquickmart.modules.inventory.dto.StockUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin inventory console. All changes write stock and an audit record. */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@Tag(name = "Admin inventory", description = "Stock levels, adjustments and history")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<List<InventoryResponse>> all(HttpServletRequest request) {
        return ApiResponses.success(inventoryService.list(), request);
    }

    @GetMapping("/{productPublicId}")
    public ApiResponse<InventoryResponse> one(@PathVariable String productPublicId,
                                              HttpServletRequest request) {
        return ApiResponses.success(inventoryService.getByProductPublicId(productPublicId), request);
    }

    @GetMapping("/{productPublicId}/history")
    public ApiResponse<List<AdjustmentResponse>> history(@PathVariable String productPublicId,
                                                         HttpServletRequest request) {
        return ApiResponses.success(inventoryService.history(productPublicId), request);
    }

    @PutMapping("/{productPublicId}/stock")
    public ApiResponse<InventoryResponse> setStock(
            @PathVariable String productPublicId,
            @Valid @RequestBody StockUpdateRequest body,
            HttpServletRequest request) {
        return ApiResponses.success(inventoryService.setStock(productPublicId, body), request);
    }

    @PostMapping("/{productPublicId}/adjust")
    public ApiResponse<InventoryResponse> adjust(
            @PathVariable String productPublicId,
            @Valid @RequestBody StockAdjustmentRequest body,
            HttpServletRequest request) {
        return ApiResponses.success(inventoryService.adjust(productPublicId, body), request);
    }
}
