package com.x.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "product_attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "attribute_name")
    private String attributeName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attribute_stores", joinColumns = @JoinColumn(name = "attribute_id"))
    @Column(name = "store_id")
    @Builder.Default
    private Set<Long> storeIds = new LinkedHashSet<>();

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_global", nullable = false)
    @Builder.Default
    private Boolean isGlobal = true;

    @Builder.Default
    private Integer status = 1;

    @Transient
    private Integer usedOnProducts;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<ProductAttributeValue> values = new ArrayList<>();
}
