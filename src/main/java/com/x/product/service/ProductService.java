package com.x.product.service;

import com.x.product.entity.Product;
import com.x.product.entity.ProductSaleChannel;
import com.x.product.entity.ProductVariant;
import com.x.product.entity.ProductImage;
import com.x.product.repository.ProductRepository;
import com.x.product.repository.ProductVariantRepository;
import com.x.product.repository.SupplierRepository;
import com.x.product.dto.ProductVariantSaleResponse;
import com.x.product.dto.MarketplaceProductResponse;
import com.x.redis.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Currency;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SupplierRepository supplierRepository;

    /**
     * List cache — short TTL. Evicted on any product write.
     */
    @Cacheable(cacheNames = CacheNames.PRODUCTS, key = "#storeId + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Long storeId, int page, int size) {
        validateStoreId(storeId);
        return productRepository.findAllByStoreId(storeId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Cacheable(cacheNames = CacheNames.PRODUCTS,
            key = "'marketplace:' + (#search == null ? '' : #search.trim().toLowerCase()) + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<MarketplaceProductResponse> getMarketplaceProducts(String search, int page, int size) {
        String normalizedSearch = search == null ? null : search.trim();
        Page<Product> products = productRepository.findMarketplaceProducts(
                normalizedSearch,
                PageRequest.of(page, size));
        return products.map(this::toMarketplaceProduct);
    }

    private MarketplaceProductResponse toMarketplaceProduct(Product product) {
        ProductVariant variant = product.getVariants() == null ? null : product.getVariants().stream()
                .filter(candidate -> candidate.getStatus() == null || candidate.getStatus() == 1)
                .filter(candidate -> candidate.getOnlinePrice() != null || candidate.getPosPrice() != null)
                .sorted((left, right) -> Boolean.TRUE.equals(right.getIsDefault())
                        ? 1 : Boolean.TRUE.equals(left.getIsDefault()) ? -1 : 0)
                .findFirst()
                .orElse(null);
        return new MarketplaceProductResponse(
                product.getId(), product.getStoreId(), product.getProductName(), product.getShortName(),
                product.getThumbnail(), product.getDescription(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getCategoryName(),
                product.getCurrencyCode(), variant == null ? null
                        : variant.getOnlinePrice() != null ? variant.getOnlinePrice() : variant.getPosPrice(),
                variant == null ? null : variant.getCompareAtPrice(),
                variant == null ? null : variant.getQuantity(), product.getIsFeatured());
    }

    /**
     * By-id cache — avoids repeated MySQL hits for the same product.
     */
    @Cacheable(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#id")
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional(readOnly = true)
    public ProductVariantSaleResponse getSellableVariant(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product variant not found"));
        Product product = variant.getProduct();
        if (!Boolean.TRUE.equals(product.getIsSellable())
                || (product.getStatus() != null && product.getStatus() != 1)
                || (variant.getStatus() != null && variant.getStatus() != 1)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product variant is not sellable");
        }
        return new ProductVariantSaleResponse(
                product.getId(), variant.getId(), product.getStoreId(), product.getProductName(),
                variant.getVariantName(), variant.getSku(), variant.getBarcode(), product.getCurrencyCode(),
                product.getSalesChannel(), variant.getCostPrice(), variant.getPosPrice(),
                variant.getOnlinePrice(), variant.getStatus());
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true)
    })
    @Transactional
    public Product createProduct(Product product) {
        validateStoreOwnership(product);
        normalizeAndValidateCurrency(product);
        prepareImages(product);
        prepareVariants(product);
        validateSalesChannelPrices(product);
        Product saved = productRepository.save(product);
        return saved;
    }

    /**
     * Keeps the catalog model consistent: a product is never directly sold;
     * its variants are sold. Even a product without options must provide one
     * default variant containing SKU, barcode and channel prices.
     */
    private void prepareVariants(Product product) {
        List<ProductVariant> variants = product.getVariants();
        if (variants == null || variants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one product variant is required");
        }

        List<ProductVariant> activeVariants = variants.stream()
                .filter(variant -> variant.getStatus() == null || variant.getStatus() == 1)
                .toList();
        long defaultVariantCount = activeVariants.stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsDefault()))
                .count();
        if (defaultVariantCount > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A product can have only one default variant");
        }
        if (defaultVariantCount == 0 && activeVariants.size() == 1) {
            activeVariants.get(0).setIsDefault(true);
        } else if (defaultVariantCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A product with multiple variants must have one default variant");
        }

        for (ProductVariant variant : variants) {
            if (variant.getSku() == null || variant.getSku().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is required for each variant");
            }
            if (variant.getBarcode() == null || variant.getBarcode().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Barcode is required for each variant");
            }
            if (variant.getSupplier() != null) {
                if (variant.getSupplier().getId() == null || !supplierRepository.existsById(variant.getSupplier().getId())) {
                    variant.setSupplier(null);
                }
            }
            variant.setProduct(product);
        }
    }

    private void prepareImages(Product product) {
        if (product.getImages() == null) {
            return;
        }
        for (ProductImage image : product.getImages()) {
            image.setProduct(product);
        }
    }

    private void validateSalesChannelPrices(Product product) {
        ProductSaleChannel salesChannel = product.getSalesChannel();
        if (salesChannel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product salesChannel is required: 1 (POS), 2 (ONLINE), or 3 (BOTH)");
        }

        for (ProductVariant variant : product.getVariants()) {
            boolean requiresPosPrice = salesChannel == ProductSaleChannel.POS || salesChannel == ProductSaleChannel.BOTH;
            boolean requiresOnlinePrice = salesChannel == ProductSaleChannel.ONLINE || salesChannel == ProductSaleChannel.BOTH;
            if (requiresPosPrice && variant.getPosPrice() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "POS price is required for each variant when salesChannel is POS or BOTH");
            }
            if (requiresOnlinePrice && variant.getOnlinePrice() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Online price is required for each variant when salesChannel is ONLINE or BOTH");
            }
        }
    }

    private void normalizeAndValidateCurrency(Product product) {
        String currencyCode = product.getCurrencyCode();
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product currencyCode is required");
        }

        String normalizedCurrencyCode = currencyCode.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalizedCurrencyCode);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product currencyCode must be a valid ISO 4217 code");
        }
        product.setCurrencyCode(normalizedCurrencyCode);
    }

    private void validateStoreOwnership(Product product) {
        validateStoreId(product.getStoreId());
    }

    private void validateStoreId(Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product storeId is required");
        }
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true)
    })
    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        product.setProductName(productDetails.getProductName());
        if (productDetails.getCurrencyCode() != null) {
            normalizeAndValidateCurrency(productDetails);
            product.setCurrencyCode(productDetails.getCurrencyCode());
        }
        if (productDetails.getSalesChannel() != null) {
            product.setSalesChannel(productDetails.getSalesChannel());
        }
        product.setShortName(productDetails.getShortName());
        product.setQrCode(productDetails.getQrCode());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setUnit(productDetails.getUnit());
        product.setTax(productDetails.getTax());
        product.setThumbnail(productDetails.getThumbnail());
        product.setDescription(productDetails.getDescription());
        product.setWeight(productDetails.getWeight());
        product.setIsFeatured(productDetails.getIsFeatured());
        product.setIsSellable(productDetails.getIsSellable());
        product.setIsStockable(productDetails.getIsStockable());
        product.setStatus(productDetails.getStatus());
        if (productDetails.getVariants() != null) updateVariants(product, productDetails.getVariants());
        if (productDetails.getImages() != null) {
            if (product.getImages() == null) {
                product.setImages(new ArrayList<>());
            } else {
                product.getImages().clear();
            }
            product.getImages().addAll(productDetails.getImages());
            prepareImages(product);
        }
        validateSalesChannelPrices(product);
        return productRepository.save(product);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true)
    })
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        product.setIsSellable(false);
        product.setStatus(0);
        product.getVariants().forEach(variant -> variant.setStatus(0));
        productRepository.save(product);
    }

    private void updateVariants(Product product, List<ProductVariant> requestedVariants) {
        if (requestedVariants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one product variant is required");
        }
        List<ProductVariant> existing = product.getVariants() == null ? new ArrayList<>() : product.getVariants();
        if (product.getVariants() == null) product.setVariants(existing);
        Map<Long, ProductVariant> existingById = existing.stream().filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        List<Long> requestedIds = requestedVariants.stream().map(ProductVariant::getId)
                .filter(Objects::nonNull).toList();
        if (requestedIds.size() != Set.copyOf(requestedIds).size()
                || !existingById.keySet().containsAll(requestedIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Each updated variant ID must belong to this product and be supplied once");
        }
        existing.stream().filter(current -> !requestedIds.contains(current.getId()))
                .forEach(current -> {
                    current.setStatus(0);
                    current.setIsDefault(false);
                });
        for (ProductVariant requested : requestedVariants) {
            if (requested.getId() == null) {
                requested.setProduct(product);
                existing.add(requested);
            } else {
                copyVariant(existingById.get(requested.getId()), requested);
            }
        }
        prepareVariants(product);
    }

    private void copyVariant(ProductVariant target, ProductVariant source) {
        target.setVariantName(source.getVariantName());
        target.setSku(source.getSku());
        target.setBarcode(source.getBarcode());
        target.setIsDefault(source.getIsDefault());
        target.setImage(source.getImage());
        target.setCostPrice(source.getCostPrice());
        target.setPosPrice(source.getPosPrice());
        target.setOnlinePrice(source.getOnlinePrice());
        target.setStockAlertQty(source.getStockAlertQty());
        target.setQuantity(source.getQuantity());
        target.setStatus(source.getStatus());
    }
}
