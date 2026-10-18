package com.x.product.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Changes to a supplier; storeIds empty means the supplier serves every store. */
public record SupplierRequest(
        @Size(max = 64) String supplierCode,
        @NotBlank @Size(max = 160) String supplierName,
        @Size(max = 160) String contactName,
        @Size(max = 32) String phone,
        @Email @Size(max = 254) String email,
        @Size(max = 1000) String address,
        @Size(max = 64) String paymentTerms,
        @Pattern(regexp = "ACTIVE|INACTIVE") String status,
        Set<Long> storeIds) {
}
