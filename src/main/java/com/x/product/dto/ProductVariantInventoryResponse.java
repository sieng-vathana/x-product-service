package com.x.product.dto;

public record ProductVariantInventoryResponse(
        Long variantId,
        Long storeId,
        String productName,
        String variantName,
        String sku,
        Integer quantity) {
}
