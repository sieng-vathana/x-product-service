package com.x.product.controller;

import com.sharedlib.response.ApiResponse;
import com.sharedlib.response.PageResponse;
import com.x.product.dto.MarketplaceCategoryResponse;
import com.x.product.dto.MarketplaceProductResponse;
import com.x.product.service.ProductCategoryService;
import com.x.product.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
@Validated
public class MarketplaceController {
    private final ProductService productService;
    private final ProductCategoryService productCategoryService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PageResponse<MarketplaceProductResponse>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        var products = productService.getMarketplaceProducts(search, page, size);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), new PageResponse<>(
                products.getContent(), products.getNumber(), products.getSize(), products.getTotalElements(),
                products.getTotalPages(), products.hasNext())));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<PageResponse<MarketplaceCategoryResponse>>> getCategories(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        var categories = productCategoryService.listForMarketplace(page, size);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), new PageResponse<>(
                categories.getContent(), categories.getNumber(), categories.getSize(), categories.getTotalElements(),
                categories.getTotalPages(), categories.hasNext())));
    }
}
