package com.x.product.repository;

import com.x.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    List<ProductVariant> findByProductIdAndStatusNot(Long productId, Integer status);
    Optional<ProductVariant> findBySku(String sku);
    Optional<ProductVariant> findByBarcode(String barcode);
    boolean existsBySkuAndIdNot(String sku, Long id);
    boolean existsByBarcodeAndIdNot(String barcode, Long id);
}
