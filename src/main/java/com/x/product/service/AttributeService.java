package com.x.product.service;

import com.x.product.entity.ProductAttribute;
import com.x.product.entity.ProductAttributeValue;
import com.x.product.repository.ProductAttributeRepository;
import com.x.product.repository.ProductAttributeValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttributeService {
    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeValueRepository attributeValueRepository;

    @Transactional(readOnly = true)
    public List<ProductAttribute> getAttributesByBusiness(Long businessId) {
        return attributeRepository.findAllByBusinessIdAndStatusNot(businessId, 0);
    }

    @Transactional(readOnly = true)
    public ProductAttribute getAttributeById(Long id) {
        return attributeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attribute not found with id: " + id));
    }

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

        if (attribute.getValues() != null) {
            attribute.getValues().forEach(v -> v.setAttribute(attribute));
        }

        return attributeRepository.save(attribute);
    }

    @Transactional
    public ProductAttribute updateAttribute(Long id, ProductAttribute attributeDetails) {
        ProductAttribute existing = getAttributeById(id);

        if (attributeDetails.getAttributeName() != null && !attributeDetails.getAttributeName().isBlank()) {
            existing.setAttributeName(attributeDetails.getAttributeName());
        }

        if (attributeDetails.getStatus() != null) {
            existing.setStatus(attributeDetails.getStatus());
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

    @Transactional
    public ProductAttribute softDeleteAttribute(Long id) {
        ProductAttribute attribute = getAttributeById(id);
        attribute.setStatus(0); // 0 = Soft deleted / Inactive
        return attributeRepository.save(attribute);
    }

    @Transactional(readOnly = true)
    public List<ProductAttributeValue> getAttributeValues(Long attributeId) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attribute not found with id: " + attributeId);
        }
        return attributeValueRepository.findAllByAttributeId(attributeId);
    }

    @Transactional
    public ProductAttributeValue addAttributeValue(Long attributeId, ProductAttributeValue value) {
        ProductAttribute attribute = getAttributeById(attributeId);
        value.setAttribute(attribute);
        return attributeValueRepository.save(value);
    }

    @Transactional
    public void deleteAttributeValue(Long valueId) {
        ProductAttributeValue val = attributeValueRepository.findById(valueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attribute value not found with id: " + valueId));
        attributeValueRepository.delete(val);
    }
}
