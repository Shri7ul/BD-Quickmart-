package com.bdquickmart.modules.catalog;

import com.bdquickmart.common.exception.BadRequestException;
import com.bdquickmart.common.exception.BusinessException;
import com.bdquickmart.common.exception.DuplicateResourceException;
import com.bdquickmart.common.exception.ResourceNotFoundException;
import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.modules.catalog.dto.CategoryRequest;
import com.bdquickmart.modules.catalog.dto.CategoryResponse;
import com.bdquickmart.modules.catalog.dto.ProductRequest;
import com.bdquickmart.modules.catalog.dto.ProductResponse;
import com.bdquickmart.modules.inventory.InventoryEntity;
import com.bdquickmart.modules.inventory.InventoryRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Category and product administration plus the customer catalogue. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final Clock clock;

    public CatalogService(CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          Clock clock) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------
    // Categories
    // ------------------------------------------------------------------

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new DuplicateResourceException("A category with that name already exists");
        }
        CategoryEntity entity = new CategoryEntity(UUID.randomUUID().toString(), name,
                request.description(), request.imageUrl(), request.active());
        return toResponse(categoryRepository.save(entity));
    }

    @Transactional
    public CategoryResponse updateCategory(String publicId, CategoryRequest request) {
        CategoryEntity entity = findCategory(publicId);
        String name = request.name().trim();
        categoryRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(entity.getId()))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("A category with that name already exists");
                });
        entity.update(name, request.description(), request.imageUrl(), request.active());
        return toResponse(categoryRepository.save(entity));
    }

    @Transactional
    public void deleteCategory(String publicId) {
        CategoryEntity entity = findCategory(publicId);
        boolean inUse = productRepository.findAllByDeletedAtIsNullOrderByNameAsc().stream()
                .anyMatch(p -> entity.getId().equals(p.getCategoryId()));
        if (inUse) {
            throw new BusinessException(ApiCode.CONFLICT,
                    "Category is in use by products; reassign them before deleting");
        }
        entity.softDelete(clock.instant());
        categoryRepository.save(entity);
    }

    public List<CategoryResponse> listCategories(boolean activeOnly) {
        List<CategoryEntity> categories = activeOnly
                ? categoryRepository.findAllByDeletedAtIsNullOrderByNameAsc()
                : categoryRepository.findAll();
        return categories.stream().map(this::toResponse).toList();
    }

    public CategoryResponse getCategory(String publicId) {
        return toResponse(findCategory(publicId));
    }

    private CategoryEntity findCategory(String publicId) {
        return categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "publicId", publicId));
    }

    private CategoryResponse toResponse(CategoryEntity entity) {
        return new CategoryResponse(entity.getId(), entity.getPublicId(), entity.getName(), entity.getDescription(),
                entity.getImageUrl(), entity.isActive(), entity.getCreatedAt());
    }

    // ------------------------------------------------------------------
    // Products
    // ------------------------------------------------------------------

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        validateProduct(request);
        Long categoryId = resolveCategory(request.categoryId());
        if (request.sku() != null && !request.sku().isBlank()
                && productRepository.findBySkuIgnoreCase(request.sku().trim()).isPresent()) {
            throw new DuplicateResourceException("A product with that SKU already exists");
        }
        ProductEntity entity = new ProductEntity(UUID.randomUUID().toString(), request.name().trim(),
                request.description(), normalise(request.sku()), categoryId,
                request.basePrice(), request.discountPrice(), request.imageUrl(), request.available());
        ProductEntity saved = productRepository.save(entity);
        // Every product always has an inventory row so stock is never nullable.
        inventoryRepository.save(new InventoryEntity(saved.getId(), 0, 5, clock.instant()));
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(String publicId, ProductRequest request) {
        ProductEntity entity = findProduct(publicId);
        validateProduct(request);
        Long categoryId = resolveCategory(request.categoryId());
        productRepository.findBySkuIgnoreCase(normalise(request.sku()) == null ? "" : normalise(request.sku()))
                .filter(other -> !other.getId().equals(entity.getId()))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("A product with that SKU already exists");
                });
        entity.update(request.name().trim(), request.description(), normalise(request.sku()),
                categoryId, request.basePrice(), request.discountPrice(),
                request.imageUrl(), request.available());
        return toResponse(productRepository.save(entity));
    }

    @Transactional
    public void deleteProduct(String publicId) {
        ProductEntity entity = findProduct(publicId);
        entity.softDelete(clock.instant());
        productRepository.save(entity);
    }

    public ProductResponse getProduct(String publicId) {
        return toResponse(findProduct(publicId));
    }

    public List<ProductResponse> listProducts(String search, Long categoryId, Boolean availableOnly) {
        List<ProductEntity> products;
        if (search != null && !search.isBlank()) {
            products = productRepository.findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        } else if (categoryId != null) {
            products = productRepository.findByDeletedAtIsNullAndCategoryIdOrderByNameAsc(categoryId);
        } else {
            products = productRepository.findAllByDeletedAtIsNullOrderByNameAsc();
        }
        return products.stream()
                .filter(p -> availableOnly == null || p.isAvailable() == availableOnly)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Products visible to customers: live and available. Out-of-stock products stay
     * listed with {@code inStock=false} so clients can render an "Out of stock"
     * state; checkout still rejects them via the stock check in OrderService.
     */
    public List<ProductResponse> listCustomerProducts(String search, Long categoryId) {
        return listProducts(search, categoryId, true);
    }

    public ProductResponse toResponse(ProductEntity product) {
        String categoryName = null;
        if (product.getCategoryId() != null) {
            categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(CategoryEntity::getName).orElse(null);
        }
        int stock = inventoryRepository.findByProductId(product.getId())
                .map(InventoryEntity::getQuantityOnHand).orElse(0);
        BigDecimal price = product.effectivePrice();
        return new ProductResponse(product.getPublicId(), product.getName(), product.getDescription(),
                product.getSku(), product.getCategoryId(), categoryName, product.getBasePrice(),
                product.getDiscountPrice(), price, product.getImageUrl(), product.isAvailable(),
                stock, product.isAvailable() && stock > 0);
    }

    private ProductEntity findProduct(String publicId) {
        return productRepository.findByPublicId(publicId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "publicId", publicId));
    }

    private void validateProduct(ProductRequest request) {
        if (request.basePrice() == null || request.basePrice().signum() <= 0) {
            throw new BadRequestException("Base price must be greater than zero");
        }
        if (request.discountPrice() != null && request.discountPrice().compareTo(request.basePrice()) >= 0) {
            throw new BadRequestException("Discount price must be lower than base price");
        }
    }

    private Long resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        CategoryEntity category = categoryRepository.findById(categoryId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", String.valueOf(categoryId)));
        return category.getId();
    }

    private String normalise(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
