package com.x.product.dto;

import com.x.product.entity.ProductSaleChannel;

import java.math.BigDecimal;

public record ProductVariantSaleResponse(
        Long productId,
        Long variantId,
        Long storeId,
        String productName,
        String variantName,
        String sku,
        String barcode,
        String currencyCode,
        ProductSaleChannel salesChannel,
        BigDecimal costPrice,
        BigDecimal posPrice,
        BigDecimal onlinePrice,
        Integer status,
        Integer quantity) {
}
