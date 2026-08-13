package com.x.product.controller;

import com.x.product.entity.ProductAttribute;
import com.x.product.entity.ProductAttributeValue;
import com.x.product.service.AttributeService;
import com.sharedlib.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/attributes")
@RequiredArgsConstructor
public class AttributeController {
    private final AttributeService attributeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductAttribute>>> getAttributes(
            @RequestParam @jakarta.validation.constraints.Positive Long businessId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                attributeService.getAttributesByBusiness(businessId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductAttribute>> getAttribute(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                attributeService.getAttributeById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductAttribute>> createAttribute(@RequestBody ProductAttribute attribute) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Product option created",
                        attributeService.createAttribute(attribute)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductAttribute>> updateAttribute(
            @PathVariable Long id,
            @RequestBody ProductAttribute attributeDetails) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Product option updated",
                attributeService.updateAttribute(id, attributeDetails)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductAttribute>> softDeleteAttribute(@PathVariable Long id) {
        ProductAttribute deleted = attributeService.softDeleteAttribute(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Product option deleted", deleted));
    }

    @GetMapping("/{attributeId}/values")
    public ResponseEntity<ApiResponse<List<ProductAttributeValue>>> getAttributeValues(@PathVariable Long attributeId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                attributeService.getAttributeValues(attributeId)));
    }

    @PostMapping("/{attributeId}/values")
    public ResponseEntity<ApiResponse<ProductAttributeValue>> addAttributeValue(
            @PathVariable Long attributeId,
            @RequestBody ProductAttributeValue value) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Option value added",
                        attributeService.addAttributeValue(attributeId, value)));
    }

    @DeleteMapping("/values/{valueId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttributeValue(@PathVariable Long valueId) {
        attributeService.deleteAttributeValue(valueId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Option value deleted", null));
    }
}
