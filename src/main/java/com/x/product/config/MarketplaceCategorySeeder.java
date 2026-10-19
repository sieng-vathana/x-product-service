package com.x.product.config;

import com.x.product.entity.ProductCategory;
import com.x.product.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Creates the initial marketplace taxonomy once; subsequent changes live in the database. */
@Component
@RequiredArgsConstructor
public class MarketplaceCategorySeeder implements ApplicationRunner {
    private static final long PLATFORM_BUSINESS_ID = 1L;

    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory("CAFE_AND_BEVERAGE", "Café & Drinks", "/categories/cafe.svg", true, 0),
            new DefaultCategory("RESTAURANT", "Food & Dining", "/categories/food.svg", true, 1),
            new DefaultCategory("BAKERY", "Bakery & Desserts", "/categories/bakery.svg", true, 2),
            new DefaultCategory("GROCERY", "Groceries & Minimart", "/categories/grocery.svg", true, 3),
            new DefaultCategory("FASHION", "Fashion & Accessories", "/categories/fashion.svg", true, 4),
            new DefaultCategory("HEALTH_BEAUTY", "Health & Pharmacy", "/categories/pharmacy.svg", false, 5),
            new DefaultCategory("ELECTRONICS", "Electronics & Tech", "/categories/electronics.svg", false, 6),
            new DefaultCategory("GENERAL_RETAIL", "General Retail", "/categories/retail.svg", false, 7)
    );

    private final ProductCategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.countByBusinessId(PLATFORM_BUSINESS_ID) > 0) {
            return;
        }

        List<ProductCategory> defaults = DEFAULT_CATEGORIES.stream()
                .map(category -> ProductCategory.builder()
                        .businessId(PLATFORM_BUSINESS_ID)
                        .categoryCode(category.code())
                        .categoryName(category.name())
                        .image(category.image())
                        .sortOrder(category.sortOrder())
                        .isFeatured(category.featured())
                        .isGlobal(true)
                        .status("ACTIVE")
                        .build())
                .toList();
        categoryRepository.saveAll(defaults);
    }

    private record DefaultCategory(String code, String name, String image, boolean featured, int sortOrder) {
    }
}
