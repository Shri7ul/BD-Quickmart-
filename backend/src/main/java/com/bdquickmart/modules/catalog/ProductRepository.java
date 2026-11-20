package com.bdquickmart.modules.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByPublicId(String publicId);

    Optional<ProductEntity> findBySkuIgnoreCase(String sku);

    List<ProductEntity> findAllByDeletedAtIsNullOrderByNameAsc();

    List<ProductEntity> findByDeletedAtIsNullAndCategoryIdOrderByNameAsc(Long categoryId);

    List<ProductEntity> findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(String name);
}
