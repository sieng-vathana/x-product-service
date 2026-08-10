package com.x.product.repository;

import com.x.product.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    @Query("""
            SELECT c FROM ProductCategory c
            WHERE c.status <> 'DELETED'
            ORDER BY c.isFeatured DESC, c.sortOrder ASC, c.categoryName ASC
            """)
    Page<ProductCategory> findMarketplaceCategories(Pageable pageable);

    Page<ProductCategory> findByBusinessIdAndStatusNot(
            Long businessId, String status, Pageable pageable);

    @Query("""
        SELECT DISTINCT c FROM ProductCategory c
        LEFT JOIN c.storeIds s
        WHERE c.businessId = :businessId
          AND c.status != :status
          AND (c.isGlobal = true OR s = :storeId)
    """)
    Page<ProductCategory> findAvailableCategories(
            @Param("businessId") Long businessId,
            @Param("storeId") Long storeId,
            @Param("status") String status,
            Pageable pageable);

    Optional<ProductCategory> findByIdAndBusinessIdAndStatusNot(
            Long id, Long businessId, String status);

    boolean existsByBusinessIdAndCategoryCodeIgnoreCaseAndStatusNot(
            Long businessId, String categoryCode, String status);

    boolean existsByBusinessIdAndCategoryCodeIgnoreCaseAndIdNotAndStatusNot(
            Long businessId, String categoryCode, Long id, String status);
}
