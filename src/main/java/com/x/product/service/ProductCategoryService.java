package com.x.product.service;

import com.x.product.dto.CatalogStatus;
import com.x.product.dto.ProductCategoryRequest;
import com.x.product.dto.ProductCategoryResponse;
import com.x.product.dto.MarketplaceCategoryResponse;
import com.x.product.entity.ProductCategory;
import com.x.product.repository.ProductCategoryRepository;
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
public class ProductCategoryService {
    private static final String DELETED = "DELETED";

    private final ProductCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<MarketplaceCategoryResponse> listForMarketplace(int page, int size) {
        return categoryRepository.findMarketplaceCategories(PageRequest.of(page, size))
                .map(category -> new MarketplaceCategoryResponse(
                        category.getId(), category.getCategoryName(), category.getImage(), category.getIsFeatured()));
    }

    @Transactional(readOnly = true)
    public Page<ProductCategoryResponse> list(
            Long businessId, Long storeId, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page, size,
                Sort.by("isFeatured").descending()
                        .and(Sort.by("sortOrder").ascending())
                        .and(Sort.by("categoryName").ascending()));
        Page<ProductCategory> result = storeId == null
                ? categoryRepository.findByBusinessIdAndStatusNot(businessId, DELETED, pageable)
                : categoryRepository.findAvailableCategories(businessId, storeId, DELETED, pageable);
        return result.map(ProductCategoryResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductCategoryResponse get(Long id, Long businessId) {
        return ProductCategoryResponse.from(require(id, businessId));
    }

    @Transactional
    public ProductCategoryResponse create(ProductCategoryRequest request) {
        String code = normalizeCode(request.categoryCode());
        ensureCodeAvailable(request.businessId(), code, null);
        ProductCategory category = ProductCategory.builder()
                .businessId(request.businessId())
                .build();
        applyRequest(category, request, code);
        return ProductCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public ProductCategoryResponse update(
            Long id, Long businessId, ProductCategoryRequest request) {
        validateBusinessId(businessId, request.businessId());
        ProductCategory category = require(id, businessId);
        String code = normalizeCode(request.categoryCode());
        ensureCodeAvailable(businessId, code, id);
        applyRequest(category, request, code);
        return ProductCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void softDelete(Long id, Long businessId) {
        ProductCategory category = require(id, businessId);
        category.setStatus(DELETED);
        categoryRepository.save(category);
    }

    private ProductCategory require(Long id, Long businessId) {
        return categoryRepository.findByIdAndBusinessIdAndStatusNot(id, businessId, DELETED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product category not found"));
    }

    private void ensureCodeAvailable(Long businessId, String code, Long excludedId) {
        boolean exists = excludedId == null
                ? categoryRepository.existsByBusinessIdAndCategoryCodeIgnoreCaseAndStatusNot(
                        businessId, code, DELETED)
                : categoryRepository.existsByBusinessIdAndCategoryCodeIgnoreCaseAndIdNotAndStatusNot(
                        businessId, code, excludedId, DELETED);
        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Product category code already exists");
        }
    }

    private void applyRequest(
            ProductCategory category,
            ProductCategoryRequest request,
            String normalizedCode) {
        boolean isGlobal = request.isGlobal() == null || Boolean.TRUE.equals(request.isGlobal());
        category.setCategoryCode(normalizedCode);
        category.setCategoryName(request.categoryName().trim());
        category.setDescription(trim(request.description()));
        category.setImage(trim(request.image()));
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setIsFeatured(Boolean.TRUE.equals(request.isFeatured()));
        category.setIsGlobal(isGlobal);
        category.setStatus(status(request.status()));

        if (!isGlobal) {
            category.setStoreIds(copyStoreIds(request.storeIds()));
        } else {
            category.setStoreIds(request.storeIds() != null ? new LinkedHashSet<>(request.storeIds()) : new LinkedHashSet<>());
        }
    }

    private Set<Long> copyStoreIds(Set<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "At least one store ID is required for store-specific categories");
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
