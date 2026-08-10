package com.x.product.dto;

public record MarketplaceCategoryResponse(
        Long id,
        String name,
        String image,
        Boolean featured) {
}
