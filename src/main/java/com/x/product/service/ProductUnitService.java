package com.x.product.service;

import com.x.product.dto.CatalogStatus;
import com.x.product.dto.ProductUnitRequest;
import com.x.product.dto.ProductUnitResponse;
import com.x.product.entity.ProductUnit;
import com.x.product.repository.ProductUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductUnitService {
    private static final String DELETED = "DELETED";

    private final ProductUnitRepository unitRepository;

    @Transactional(readOnly = true)
    public Page<ProductUnitResponse> list(
            Long businessId, Long storeId, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page, size, Sort.by("unitName").ascending());
        Page<ProductUnit> result = storeId == null
                ? unitRepository.findByBusinessIdAndStatusNot(businessId, DELETED, pageable)
                : unitRepository.findAvailableUnits(businessId, storeId, DELETED, pageable);
        return result.map(ProductUnitResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductUnitResponse get(Long id, Long businessId) {
        return ProductUnitResponse.from(require(id, businessId));
    }

    @Transactional
    public ProductUnitResponse create(ProductUnitRequest request) {
        String code = normalizeCode(request.unitCode());
        ensureCodeAvailable(request.businessId(), code, null);
        ProductUnit unit = ProductUnit.builder()
                .businessId(request.businessId())
                .build();
        applyRequest(unit, request, code);
        return ProductUnitResponse.from(unitRepository.save(unit));
    }

    @Transactional
    public ProductUnitResponse update(
            Long id, Long businessId, ProductUnitRequest request) {
        validateBusinessId(businessId, request.businessId());
        ProductUnit unit = require(id, businessId);
        String code = normalizeCode(request.unitCode());
        ensureCodeAvailable(businessId, code, id);
        applyRequest(unit, request, code);
        return ProductUnitResponse.from(unitRepository.save(unit));
    }

    @Transactional
    public void softDelete(Long id, Long businessId) {
        ProductUnit unit = require(id, businessId);
        unit.setStatus(DELETED);
        unitRepository.save(unit);
    }

    private ProductUnit require(Long id, Long businessId) {
        return unitRepository.findByIdAndBusinessIdAndStatusNot(id, businessId, DELETED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product unit not found"));
    }

    private void ensureCodeAvailable(Long businessId, String code, Long excludedId) {
        boolean exists = excludedId == null
                ? unitRepository.existsByBusinessIdAndUnitCodeIgnoreCaseAndStatusNot(
                        businessId, code, DELETED)
                : unitRepository.existsByBusinessIdAndUnitCodeIgnoreCaseAndIdNotAndStatusNot(
                        businessId, code, excludedId, DELETED);
        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Product unit code already exists");
        }
    }

    private void applyRequest(
            ProductUnit unit, ProductUnitRequest request, String normalizedCode) {
        boolean isGlobal = request.isGlobal() == null || Boolean.TRUE.equals(request.isGlobal());
        unit.setUnitCode(normalizedCode);
        unit.setUnitName(request.unitName().trim());
        unit.setDescription(trim(request.description()));
        unit.setIsGlobal(isGlobal);
        unit.setStatus(status(request.status()));

        if (unit.getStoreIds() == null) {
            unit.setStoreIds(new LinkedHashSet<>());
        }
        unit.getStoreIds().clear();
        if (!isGlobal) {
            unit.getStoreIds().addAll(copyStoreIds(request.storeIds()));
        } else if (request.storeIds() != null) {
            unit.getStoreIds().addAll(request.storeIds());
        }
    }

    private Set<Long> copyStoreIds(Set<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "At least one store ID is required for store-specific units");
        }
        return new LinkedHashSet<>(storeIds);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String status(CatalogStatus status) {
        return (status == null ? CatalogStatus.ACTIVE : status).name();
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateBusinessId(Long expected, Long requested) {
        if (!expected.equals(requested)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "businessId cannot be changed");
        }
    }
}
