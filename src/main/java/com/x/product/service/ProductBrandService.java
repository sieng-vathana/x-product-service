package com.x.product.service;

import com.x.product.dto.CatalogStatus;
import com.x.product.dto.ProductBrandRequest;
import com.x.product.dto.ProductBrandResponse;
import com.x.product.entity.ProductBrand;
import com.x.product.repository.ProductBrandRepository;
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
public class ProductBrandService {
    private static final String DELETED = "DELETED";

    private final ProductBrandRepository brandRepository;

    @Transactional(readOnly = true)
    public Page<ProductBrandResponse> list(Long businessId, Long storeId, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page, size, Sort.by("isFeatured").descending().and(Sort.by("brandName").ascending()));
        Page<ProductBrand> result = storeId == null
                ? brandRepository.findByBusinessIdAndStatusNot(businessId, DELETED, pageable)
                : brandRepository.findAvailableBrands(businessId, storeId, DELETED, pageable);
        return result.map(ProductBrandResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductBrandResponse get(Long id, Long businessId) {
        return ProductBrandResponse.from(require(id, businessId));
    }

    @Transactional
    public ProductBrandResponse create(ProductBrandRequest request) {
        String code = normalizeCode(request.brandCode());
        ensureCodeAvailable(request.businessId(), code, null);
        ProductBrand brand = ProductBrand.builder()
                .businessId(request.businessId())
                .build();
        applyRequest(brand, request, code);
        return ProductBrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public ProductBrandResponse update(Long id, Long businessId, ProductBrandRequest request) {
        validateBusinessId(businessId, request.businessId());
        ProductBrand brand = require(id, businessId);
        String code = normalizeCode(request.brandCode());
        ensureCodeAvailable(businessId, code, id);
        applyRequest(brand, request, code);
        return ProductBrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public void softDelete(Long id, Long businessId) {
        ProductBrand brand = require(id, businessId);
        brand.setStatus(DELETED);
        brandRepository.save(brand);
    }

    private ProductBrand require(Long id, Long businessId) {
        return brandRepository.findByIdAndBusinessIdAndStatusNot(id, businessId, DELETED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product brand not found"));
    }

    private void ensureCodeAvailable(Long businessId, String code, Long excludedId) {
        boolean exists = excludedId == null
                ? brandRepository.existsByBusinessIdAndBrandCodeIgnoreCaseAndStatusNot(businessId, code, DELETED)
                : brandRepository.existsByBusinessIdAndBrandCodeIgnoreCaseAndIdNotAndStatusNot(businessId, code, excludedId, DELETED);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product brand code already exists");
        }
    }

    private void applyRequest(ProductBrand brand, ProductBrandRequest request, String normalizedCode) {
        boolean isGlobal = request.isGlobal() == null || Boolean.TRUE.equals(request.isGlobal());
        brand.setBrandCode(normalizedCode);
        brand.setBrandName(request.brandName().trim());
        brand.setDescription(trim(request.description()));
        brand.setLogo(trim(request.logo()));
        brand.setIsFeatured(Boolean.TRUE.equals(request.isFeatured()));
        brand.setIsGlobal(isGlobal);
        brand.setStatus(status(request.status()));

        if (brand.getStoreIds() == null) {
            brand.setStoreIds(new LinkedHashSet<>());
        }
        brand.getStoreIds().clear();
        if (!isGlobal) {
            brand.getStoreIds().addAll(copyStoreIds(request.storeIds()));
        } else if (request.storeIds() != null) {
            brand.getStoreIds().addAll(request.storeIds());
        }
    }

    private Set<Long> copyStoreIds(Set<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one store ID is required for store-specific brands");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessId cannot be changed");
        }
    }
}
