package com.x.product.service;

import com.x.product.entity.Product;
import com.x.product.entity.ProductVariant;
import com.x.product.repository.ProductRepository;
import com.x.product.repository.ProductVariantRepository;
import com.x.product.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<ProductVariant> getVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + productId);
        }
        return productVariantRepository.findByProductIdAndStatusNot(productId, 0);
    }

    @Transactional(readOnly = true)
    public ProductVariant getVariantById(Long id) {
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product variant not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public ProductVariant getVariantBySku(String sku) {
        return productVariantRepository.findBySku(sku)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product variant not found with SKU: " + sku));
    }

    @Transactional(readOnly = true)
    public ProductVariant getVariantByBarcode(String barcode) {
        return productVariantRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product variant not found with barcode: " + barcode));
    }

    @Transactional
    public ProductVariant createVariant(Long productId, ProductVariant variant) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + productId));

        if (variant.getSku() == null || variant.getSku().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is required for a product variant");
        }
        if (variant.getBarcode() == null || variant.getBarcode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Barcode is required for a product variant");
        }

        if (productVariantRepository.findBySku(variant.getSku()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Variant with SKU '" + variant.getSku() + "' already exists");
        }
        if (productVariantRepository.findByBarcode(variant.getBarcode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Variant with Barcode '" + variant.getBarcode() + "' already exists");
        }

        sanitizeSupplier(variant);
        variant.setProduct(product);
        if (variant.getStatus() == null) {
            variant.setStatus(1);
        }

        if (variant.getAttributeValues() != null) {
            variant.getAttributeValues().forEach(av -> av.setVariant(variant));
        }

        return productVariantRepository.save(variant);
    }

    @Transactional
    public ProductVariant updateVariant(Long id, ProductVariant variantDetails) {
        ProductVariant existing = getVariantById(id);

        if (variantDetails.getSku() != null && !variantDetails.getSku().isBlank()) {
            if (productVariantRepository.existsBySkuAndIdNot(variantDetails.getSku(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU '" + variantDetails.getSku() + "' is used by another variant");
            }
            existing.setSku(variantDetails.getSku());
        }

        if (variantDetails.getBarcode() != null && !variantDetails.getBarcode().isBlank()) {
            if (productVariantRepository.existsByBarcodeAndIdNot(variantDetails.getBarcode(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Barcode '" + variantDetails.getBarcode() + "' is used by another variant");
            }
            existing.setBarcode(variantDetails.getBarcode());
        }

        if (variantDetails.getVariantName() != null) existing.setVariantName(variantDetails.getVariantName());
        if (variantDetails.getImage() != null) existing.setImage(variantDetails.getImage());
        if (variantDetails.getCostPrice() != null) existing.setCostPrice(variantDetails.getCostPrice());
        if (variantDetails.getPosPrice() != null) existing.setPosPrice(variantDetails.getPosPrice());
        if (variantDetails.getCompareAtPrice() != null) existing.setCompareAtPrice(variantDetails.getCompareAtPrice());
        if (variantDetails.getOnlinePrice() != null) existing.setOnlinePrice(variantDetails.getOnlinePrice());
        if (variantDetails.getStockAlertQty() != null) existing.setStockAlertQty(variantDetails.getStockAlertQty());
        if (variantDetails.getQuantity() != null) existing.setQuantity(variantDetails.getQuantity());
        if (variantDetails.getIsDefault() != null) existing.setIsDefault(variantDetails.getIsDefault());
        if (variantDetails.getStatus() != null) existing.setStatus(variantDetails.getStatus());

        sanitizeSupplier(variantDetails);
        if (variantDetails.getSupplier() != null) {
            existing.setSupplier(variantDetails.getSupplier());
        }

        if (variantDetails.getAttributeValues() != null) {
            existing.getAttributeValues().clear();
            variantDetails.getAttributeValues().forEach(av -> {
                av.setVariant(existing);
                existing.getAttributeValues().add(av);
            });
        }

        return productVariantRepository.save(existing);
    }

    @Transactional
    public ProductVariant updateStock(Long id, Integer quantity, Integer stockAlertQty) {
        ProductVariant existing = getVariantById(id);
        if (quantity != null) existing.setQuantity(quantity);
        if (stockAlertQty != null) existing.setStockAlertQty(stockAlertQty);
        return productVariantRepository.save(existing);
    }

    @Transactional
    public ProductVariant softDeleteVariant(Long id) {
        ProductVariant existing = getVariantById(id);
        existing.setStatus(0); // 0 = Soft deleted / Inactive
        return productVariantRepository.save(existing);
    }

    private void sanitizeSupplier(ProductVariant variant) {
        if (variant.getSupplier() != null) {
            if (variant.getSupplier().getId() == null || !supplierRepository.existsById(variant.getSupplier().getId())) {
                variant.setSupplier(null);
            }
        }
    }
}
