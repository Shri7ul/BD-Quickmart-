package com.bdquickmart.modules.customer;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.catalog.CatalogService;
import com.bdquickmart.modules.catalog.dto.CategoryResponse;
import com.bdquickmart.modules.catalog.dto.ProductResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer-facing catalogue. Only live, available products are exposed; stock is reported, not filtered. */
@RestController
@RequestMapping("/api/v1/customer")
@Tag(name = "Customer catalog", description = "Browse categories and products")
public class CustomerCatalogController {

    private final CatalogService catalogService;

    public CustomerCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories(HttpServletRequest request) {
        return ApiResponses.success(catalogService.listCategories(true), request);
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductResponse>> products(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            HttpServletRequest request) {
        return ApiResponses.success(catalogService.listCustomerProducts(search, categoryId), request);
    }

    @GetMapping("/products/{publicId}")
    public ApiResponse<ProductResponse> product(@PathVariable String publicId,
                                                 HttpServletRequest request) {
        return ApiResponses.success(catalogService.getProduct(publicId), request);
    }
}
