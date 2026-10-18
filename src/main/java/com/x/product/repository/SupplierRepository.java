package com.x.product.repository;

import com.x.product.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Page<Supplier> findByBusinessIdAndStatusNot(Long businessId, String status, Pageable pageable);

    @Query("""
        SELECT DISTINCT s FROM Supplier s
        LEFT JOIN s.storeIds st
        WHERE s.businessId = :businessId
          AND (s.status IS NULL OR s.status != :status)
          AND (st IS NULL OR st = :storeId)
    """)
    Page<Supplier> findAvailableSuppliers(
            @Param("businessId") Long businessId,
            @Param("storeId") Long storeId,
            @Param("status") String status,
            Pageable pageable);

    Optional<Supplier> findByIdAndBusinessIdAndStatusNot(Long id, Long businessId, String status);

    boolean existsByBusinessIdAndSupplierCode(Long businessId, String supplierCode);
}
