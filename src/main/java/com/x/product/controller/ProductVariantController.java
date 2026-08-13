package com.x.product.controller;

import com.sharedlib.response.ApiResponse;
import com.x.product.entity.ProductVariant;
import com.x.product.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/variants")
@RequiredArgsConstructor
@Validated
public class ProductVariantController {
    private final ProductVariantService productVariantService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ProductVariant>>> getVariantsByProductId(@PathVariable Long productId) {
        List<ProductVariant> variants = productVariantService.getVariantsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), variants));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductVariant>> getVariantById(@PathVariable Long id) {
        ProductVariant variant = productVariantService.getVariantById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), variant));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductVariant>> getVariantBySku(@PathVariable String sku) {
        ProductVariant variant = productVariantService.getVariantBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), variant));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<ProductVariant>> getVariantByBarcode(@PathVariable String barcode) {
        ProductVariant variant = productVariantService.getVariantByBarcode(barcode);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), variant));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<ProductVariant>> createVariant(
            @PathVariable Long productId,
            @RequestBody ProductVariant variant) {
        ProductVariant created = productVariantService.createVariant(productId, variant);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Variant created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductVariant>> updateVariant(
            @PathVariable Long id,
            @RequestBody ProductVariant variantDetails) {
        ProductVariant updated = productVariantService.updateVariant(id, variantDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Variant updated successfully", updated));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductVariant>> updateStock(
            @PathVariable Long id,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) Integer stockAlertQty) {
        ProductVariant updated = productVariantService.updateStock(id, quantity, stockAlertQty);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Variant stock updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductVariant>> softDeleteVariant(@PathVariable Long id) {
        ProductVariant deleted = productVariantService.softDeleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Variant soft deleted successfully", deleted));
    }
}
