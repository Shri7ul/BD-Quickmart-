package com.bdquickmart.modules.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findByPublicId(String publicId);

    Optional<CategoryEntity> findByNameIgnoreCase(String name);

    List<CategoryEntity> findAllByDeletedAtIsNullOrderByNameAsc();
}
