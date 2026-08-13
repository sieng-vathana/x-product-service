package com.x.product.repository;

import com.x.product.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
    List<ProductAttribute> findAllByBusinessId(Long businessId);
    List<ProductAttribute> findAllByBusinessIdAndStatusNot(Long businessId, Integer status);
}
