package com.x.product.service;

import com.x.product.entity.ProductAttribute;
import com.x.product.entity.ProductAttributeValue;
import com.x.product.repository.ProductAttributeRepository;
import com.x.product.repository.ProductAttributeValueRepository;
import com.x.product.repository.ProductVariantAttributeValueRepository;
import com.x.redis.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttributeService {
    private static final String ATTRIBUTES_CACHE = CacheNames.PRODUCT_ATTRIBUTES;
    private static final String ATTRIBUTE_BY_ID_CACHE = CacheNames.PRODUCT_ATTRIBUTE_BY_ID;
    private static final String ATTRIBUTE_VALUES_CACHE = CacheNames.PRODUCT_ATTRIBUTE_VALUES;

    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeValueRepository attributeValueRepository;
    private final ProductVariantAttributeValueRepository variantAttributeValueRepository;

    @Cacheable(cacheNames = ATTRIBUTES_CACHE, key = "#businessId")
    @Transactional(readOnly = true)
    public List<ProductAttribute> getAttributesByBusiness(Long businessId) {
        List<ProductAttribute> attributes = attributeRepository.findAllByBusinessIdAndStatusNot(businessId, 0);
        attributes.forEach(this::populateUsageCount);
        return attributes;
    }

    @Cacheable(cacheNames = ATTRIBUTE_BY_ID_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public ProductAttribute getAttributeById(Long id) {
        ProductAttribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attribute not found with id: " + id));
        populateUsageCount(attribute);
        return attribute;
    }

    @CacheEvict(cacheNames = ATTRIBUTES_CACHE, allEntries = true)
    @Transactional
    public ProductAttribute createAttribute(ProductAttribute attribute) {
        if (attribute.getBusinessId() == null || attribute.getBusinessId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute businessId is required");
        }
        if (attribute.getAttributeName() == null || attribute.getAttributeName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute name is required");
        }

        if (attribute.getStatus() == null) {
            attribute.setStatus(1);
        }
        if (attribute.getIsGlobal() == null) {
            attribute.setIsGlobal(true);
        }
        if (attribute.getStoreIds() == null) {
            attribute.setStoreIds(new LinkedHashSet<>());
        }

        if (attribute.getValues() != null) {
            attribute.getValues().forEach(v -> v.setAttribute(attribute));
        }

        return attributeRepository.save(attribute);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = ATTRIBUTES_CACHE, allEntries = true),
            @CacheEvict(cacheNames = ATTRIBUTE_BY_ID_CACHE, key = "#id"),
            @CacheEvict(cacheNames = ATTRIBUTE_VALUES_CACHE, key = "#id")
    })
    @Transactional
    public ProductAttribute updateAttribute(Long id, ProductAttribute attributeDetails) {
        ProductAttribute existing = getAttributeById(id);

        if (attributeDetails.getAttributeName() != null && !attributeDetails.getAttributeName().isBlank()) {
            existing.setAttributeName(attributeDetails.getAttributeName());
        }

        if (attributeDetails.getStatus() != null) {
            existing.setStatus(attributeDetails.getStatus());
        }

        if (attributeDetails.getDescription() != null) {
            existing.setDescription(attributeDetails.getDescription());
        }

        if (attributeDetails.getIsGlobal() != null) {
            existing.setIsGlobal(attributeDetails.getIsGlobal());
        }

        if (attributeDetails.getStoreIds() != null) {
            existing.getStoreIds().clear();
            existing.getStoreIds().addAll(attributeDetails.getStoreIds());
        }

        if (attributeDetails.getValues() != null) {
            existing.getValues().clear();
            attributeDetails.getValues().forEach(v -> {
                v.setAttribute(existing);
                existing.getValues().add(v);
            });
        }

        return attributeRepository.save(existing);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = ATTRIBUTES_CACHE, allEntries = true),
            @CacheEvict(cacheNames = ATTRIBUTE_BY_ID_CACHE, key = "#id"),
            @CacheEvict(cacheNames = ATTRIBUTE_VALUES_CACHE, key = "#id")
    })
    @Transactional
    public ProductAttribute softDeleteAttribute(Long id) {
        ProductAttribute attribute = getAttributeById(id);
        attribute.setStatus(0); // 0 = Soft deleted / Inactive
        return attributeRepository.save(attribute);
    }

    @Cacheable(cacheNames = ATTRIBUTE_VALUES_CACHE, key = "#attributeId")
    @Transactional(readOnly = true)
    public List<ProductAttributeValue> getAttributeValues(Long attributeId) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attribute not found with id: " + attributeId);
        }
        return attributeValueRepository.findAllByAttributeId(attributeId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = ATTRIBUTES_CACHE, allEntries = true),
            @CacheEvict(cacheNames = ATTRIBUTE_BY_ID_CACHE, key = "#attributeId"),
            @CacheEvict(cacheNames = ATTRIBUTE_VALUES_CACHE, key = "#attributeId")
    })
    @Transactional
    public ProductAttributeValue addAttributeValue(Long attributeId, ProductAttributeValue value) {
        ProductAttribute attribute = getAttributeById(attributeId);
        value.setAttribute(attribute);
        return attributeValueRepository.save(value);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = ATTRIBUTES_CACHE, allEntries = true),
            @CacheEvict(cacheNames = ATTRIBUTE_BY_ID_CACHE, allEntries = true),
            @CacheEvict(cacheNames = ATTRIBUTE_VALUES_CACHE, allEntries = true)
    })
    @Transactional
    public void deleteAttributeValue(Long valueId) {
        ProductAttributeValue val = attributeValueRepository.findById(valueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attribute value not found with id: " + valueId));
        attributeValueRepository.delete(val);
    }

    private void populateUsageCount(ProductAttribute attribute) {
        attribute.setUsedOnProducts(Math.toIntExact(
                variantAttributeValueRepository.countDistinctProductsByAttributeId(attribute.getId())));
    }
}
