package com.x.product.service;

import com.x.product.dto.SupplierRequest;
import com.x.product.entity.Supplier;
import com.x.product.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierService {
    private static final String DELETED = "DELETED";
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public Page<Supplier> list(Long businessId, Long storeId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("supplierName").ascending());
        return storeId == null
                ? supplierRepository.findByBusinessIdAndStatusNot(businessId, DELETED, pageable)
                : supplierRepository.findAvailableSuppliers(businessId, storeId, DELETED, pageable);
    }

    @Transactional(readOnly = true)
    public Supplier get(Long id, Long businessId) {
        return supplierRepository.findByIdAndBusinessIdAndStatusNot(id, businessId, DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }

    @Transactional
    public Supplier create(Supplier supplier) {
        if (supplier.getStatus() == null) {
            supplier.setStatus("ACTIVE");
        }
        supplier.setSupplierCode(StringUtils.hasText(supplier.getSupplierCode())
                ? supplier.getSupplierCode().trim().toUpperCase(Locale.ROOT)
                : "SUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        ensureCodeAvailable(supplier.getBusinessId(), supplier.getSupplierCode());
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, Long businessId, SupplierRequest request) {
        Supplier supplier = get(id, businessId);
        if (StringUtils.hasText(request.supplierCode())) {
            String code = request.supplierCode().trim().toUpperCase(Locale.ROOT);
            if (!code.equals(supplier.getSupplierCode())) {
                ensureCodeAvailable(businessId, code);
                supplier.setSupplierCode(code);
            }
        }
        supplier.setSupplierName(request.supplierName().trim());
        supplier.setContactName(trim(request.contactName()));
        supplier.setPhone(trim(request.phone()));
        supplier.setEmail(trim(request.email()));
        supplier.setAddress(trim(request.address()));
        supplier.setPaymentTerms(trim(request.paymentTerms()));
        if (request.status() != null) {
            supplier.setStatus(request.status());
        }
        if (request.storeIds() != null) {
            supplier.setStoreIds(new HashSet<>(request.storeIds()));
        }
        return supplierRepository.save(supplier);
    }

    private void ensureCodeAvailable(Long businessId, String code) {
        if (supplierRepository.existsByBusinessIdAndSupplierCode(businessId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Supplier code already exists");
        }
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
