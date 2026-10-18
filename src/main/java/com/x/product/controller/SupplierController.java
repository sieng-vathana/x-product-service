package com.x.product.controller;

import com.x.product.dto.SupplierRequest;
import com.x.product.entity.Supplier;
import com.x.product.service.SupplierService;
import com.sharedlib.response.ApiResponse;
import com.sharedlib.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/products/suppliers")
@RequiredArgsConstructor
@Validated
public class SupplierController {
    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Supplier>>> list(
            @RequestParam Long businessId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Page<Supplier> result = supplierService.list(businessId, storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                new PageResponse<>(
                        result.getContent(),
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.hasNext())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> get(
            @PathVariable Long id,
            @RequestParam Long businessId) {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), supplierService.get(id, businessId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> update(
            @PathVariable Long id,
            @RequestParam Long businessId,
            @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Supplier updated", supplierService.update(id, businessId, request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Supplier>> create(@RequestBody Supplier supplier) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Supplier created", supplierService.create(supplier)));
    }
}
