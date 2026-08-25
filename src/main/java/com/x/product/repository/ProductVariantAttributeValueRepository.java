package com.x.product.repository;

import com.x.product.entity.ProductVariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantAttributeValueRepository extends JpaRepository<ProductVariantAttributeValue, Long> {
    List<ProductVariantAttributeValue> findAllByVariantId(Long variantId);

    void deleteAllByVariantId(Long variantId);

    @Query("""
            select count(distinct p.id)
            from ProductVariantAttributeValue link
            join link.variant variant
            join variant.product p
            where link.attributeValue.attribute.id = :attributeId
            """)
    long countDistinctProductsByAttributeId(@Param("attributeId") Long attributeId);
}
