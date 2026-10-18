package com.x.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "suppliers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"business_id", "supplier_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "supplier_stores", joinColumns = @JoinColumn(name = "supplier_id"))
    @Column(name = "store_id")
    @Builder.Default
    private Set<Long> storeIds = new HashSet<>();

    @Column(name = "supplier_code", nullable = false, length = 64)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 160)
    private String supplierName;

    /** The person to call at the supplier. */
    @Column(name = "contact_name", length = 160)
    private String contactName;

    @Column(length = 32)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    /** For example "Net 30" or "Cash on delivery". */
    @Column(name = "payment_terms", length = 64)
    private String paymentTerms;

    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
