package com.bdquickmart.modules.admin;

import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.bdquickmart.modules.catalog.CatalogService;
import com.bdquickmart.modules.catalog.dto.CategoryRequest;
import com.bdquickmart.modules.catalog.dto.CategoryResponse;
import com.bdquickmart.modules.catalog.dto.ProductRequest;
import com.bdquickmart.modules.catalog.dto.ProductResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin product and category management. Persists to PostgreSQL. */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin catalog", description = "Product and category management")
public class AdminCatalogController {

    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            HttpServletRequest request) {
        return ApiResponses.success(catalogService.listCategories(activeOnly), request);
    }

    @GetMapping("/categories/{publicId}")
    public ApiResponse<CategoryResponse> category(@PathVariable String publicId,
                                                   HttpServletRequest request) {
        return ApiResponses.success(catalogService.getCategory(publicId), request);
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(catalogService.createCategory(body), request));
    }

    @PutMapping("/categories/{publicId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable String publicId, @Valid @RequestBody CategoryRequest body,
            HttpServletRequest request) {
        return ApiResponses.success(catalogService.updateCategory(publicId, body), request);
    }

    @DeleteMapping("/categories/{publicId}")
    public ApiResponse<Void> deleteCategory(@PathVariable String publicId,
                                             HttpServletRequest request) {
        catalogService.deleteCategory(publicId);
        return ApiResponses.success(null, "Category deleted", request);
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductResponse>> products(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean availableOnly,
            HttpServletRequest request) {
        return ApiResponses.success(catalogService.listProducts(search, categoryId, availableOnly), request);
    }

    @GetMapping("/products/{publicId}")
    public ApiResponse<ProductResponse> product(@PathVariable String publicId,
                                                 HttpServletRequest request) {
        return ApiResponses.success(catalogService.getProduct(publicId), request);
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(catalogService.createProduct(body), request));
    }

    @PutMapping("/products/{publicId}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable String publicId, @Valid @RequestBody ProductRequest body,
            HttpServletRequest request) {
        return ApiResponses.success(catalogService.updateProduct(publicId, body), request);
    }

    @DeleteMapping("/products/{publicId}")
    public ApiResponse<Void> deleteProduct(@PathVariable String publicId,
                                            HttpServletRequest request) {
        catalogService.deleteProduct(publicId);
        return ApiResponses.success(null, "Product deleted", request);
    }
}
