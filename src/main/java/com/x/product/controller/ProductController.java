package com.x.product.controller;

import com.x.product.entity.Product;
import com.x.product.service.ProductService;
import com.x.product.dto.ProductVariantSaleResponse;
import com.sharedlib.response.ApiResponse;
import com.sharedlib.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Product>>> getAllProducts(
            @RequestParam @jakarta.validation.constraints.Positive Long storeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var products = productService.getAllProducts(storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), new PageResponse<>(
                products.getContent(), products.getNumber(), products.getSize(), products.getTotalElements(),
                products.getTotalPages(), products.hasNext())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), productService.getProductById(id)));
    }

    @GetMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<ProductVariantSaleResponse>> getSellableVariant(
            @PathVariable Long id,
            @RequestParam(required = false) @jakarta.validation.constraints.Positive Long storeId) {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), productService.getSellableVariant(id, storeId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Product created", productService.createProduct(product)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Product updated",
                productService.updateProduct(id, productDetails)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Product deleted", null));
    }
}
