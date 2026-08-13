package com.x.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "products",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_store_code",
                columnNames = {"store_id", "product_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "is_global")
    @Builder.Default
    private Boolean isGlobal = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_store_mappings", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "store_id")
    private List<Long> storeIds;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "product_name")
    private String productName;

    /**
     * ISO 4217 currency code owned by the catalog product. Every variant and
     * every channel listing for this product uses this currency.
     */
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    /**
     * 1 = POS, 2 = ONLINE, 3 = BOTH. All variants inherit this availability.
     */
    @Convert(converter = ProductSaleChannelConverter.class)
    @Column(name = "sales_channel")
    private ProductSaleChannel salesChannel;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "qr_code")
    private String qrCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id")
    private ProductBrand brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unit_id")
    private ProductUnit unit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tax_id")
    private ProductTax tax;

    @Column(columnDefinition = "TEXT")
    private String thumbnail;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal weight;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_sellable")
    @Builder.Default
    private Boolean isSellable = true;

    @Column(name = "is_stockable")
    @Builder.Default
    private Boolean isStockable = true;

    private Integer status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * A product is catalog metadata. Every sellable item is a variant, including
     * the single default variant created for products without options.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<ProductVariant> variants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<ProductImage> images;
}
