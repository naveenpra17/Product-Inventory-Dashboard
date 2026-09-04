package com.inventory.service;

import com.inventory.dto.ProductImportRow;
import com.inventory.dto.ProductResponse;
import com.inventory.dto.ProductSummaryResponse;
import com.inventory.entity.Product;
import com.inventory.mapper.ProductMapper;
import com.inventory.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Pageable pageable, String search) {
        Specification<Product> spec = buildSearchSpecification(search);
        return productRepository.findAll(spec, pageable)
                .map(product -> productMapper.toResponse(product, calculateStockAge(product.getPurchaseDate())));
    }

    @Transactional(readOnly = true)
    public ProductSummaryResponse getSummary() {
        long totalProducts = productRepository.count();
        BigDecimal totalValue = productRepository.calculateTotalInventoryValue();
        if (totalValue == null) {
            totalValue = BigDecimal.ZERO;
        } else {
            totalValue = totalValue.setScale(2, RoundingMode.HALF_UP);
        }

        List<LocalDate> purchaseDates = productRepository.findAllPurchaseDates();
        double averageStockAge = 0.0;
        if (!purchaseDates.isEmpty()) {
            long totalAge = purchaseDates.stream()
                    .mapToLong(this::calculateStockAge)
                    .sum();
            averageStockAge = (double) totalAge / purchaseDates.size();
            averageStockAge = BigDecimal.valueOf(averageStockAge)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return ProductSummaryResponse.builder()
                .totalProducts(totalProducts)
                .totalInventoryValue(totalValue)
                .averageStockAgeDays(averageStockAge)
                .build();
    }

    @Transactional
    public List<Product> saveAll(List<Product> products) {
        return productRepository.saveAll(products);
    }

    public Product toEntity(ProductImportRow row) {
        return productMapper.toEntity(row);
    }

    public long calculateStockAge(LocalDate purchaseDate) {
        LocalDate today = LocalDate.now(clock);
        return ChronoUnit.DAYS.between(purchaseDate, today);
    }

    private Specification<Product> buildSearchSpecification(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("productSku")), pattern));
            predicates.add(cb.like(cb.lower(root.get("productName")), pattern));
            predicates.add(cb.like(cb.lower(root.get("category")), pattern));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
