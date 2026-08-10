package com.x.product.repository;

import com.x.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByStoreIdAndProductCode(Long storeId, String productCode);

    Page<Product> findAllByStoreId(Long storeId, Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN p.category c
            JOIN p.variants v
            WHERE (p.status IS NULL OR p.status = 1)
              AND (p.isSellable IS NULL OR p.isSellable = true)
              AND p.salesChannel IN :onlineChannels
              AND (v.status IS NULL OR v.status = 1)
              AND v.onlinePrice IS NOT NULL
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.shortName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            ORDER BY p.createdAt DESC
            """)
    Page<Product> findMarketplaceProducts(
            @Param("search") String search,
            @Param("onlineChannels") List<com.x.product.entity.ProductSaleChannel> onlineChannels,
            Pageable pageable);
}
