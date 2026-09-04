package com.inventory.service;

import com.inventory.entity.Product;
import com.inventory.mapper.ProductMapper;
import com.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductMapper productMapper;
    private ProductService productService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapper();
        fixedClock = Clock.fixed(Instant.parse("2026-09-04T00:00:00Z"), ZoneId.of("UTC"));
        productService = new ProductService(productRepository, productMapper, fixedClock);
    }

    @Test
    void calculateStockAge_returnsDaysBetweenPurchaseDateAndToday() {
        LocalDate purchaseDate = LocalDate.of(2026, 9, 1);
        long stockAge = productService.calculateStockAge(purchaseDate);
        assertThat(stockAge).isEqualTo(3);
    }

    @Test
    void getSummary_calculatesTotalsAndAverageStockAge() {
        when(productRepository.count()).thenReturn(2L);
        when(productRepository.calculateTotalInventoryValue())
                .thenReturn(new BigDecimal("1500.00"));
        when(productRepository.findAllPurchaseDates()).thenReturn(List.of(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 8, 25)
        ));

        var summary = productService.getSummary();

        assertThat(summary.getTotalProducts()).isEqualTo(2);
        assertThat(summary.getTotalInventoryValue()).isEqualByComparingTo("1500.00");
        assertThat(summary.getAverageStockAgeDays()).isEqualTo(6.5);
    }

    @Test
    void getSummary_returnsZeroWhenNoProducts() {
        when(productRepository.count()).thenReturn(0L);
        when(productRepository.calculateTotalInventoryValue()).thenReturn(BigDecimal.ZERO);
        when(productRepository.findAllPurchaseDates()).thenReturn(List.of());

        var summary = productService.getSummary();

        assertThat(summary.getTotalProducts()).isZero();
        assertThat(summary.getTotalInventoryValue()).isEqualByComparingTo("0.00");
        assertThat(summary.getAverageStockAgeDays()).isZero();
    }

    @Test
    void getProducts_mapsEntitiesToResponses() {
        Product product = Product.builder()
                .id(1L)
                .productSku("SKU001")
                .productName("Laptop")
                .category("Electronics")
                .purchaseDate(LocalDate.of(2026, 9, 1))
                .unitPrice(new BigDecimal("750.00"))
                .quantity(5)
                .build();

        when(productRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));

        Page<com.inventory.dto.ProductResponse> page = productService.getProducts(PageRequest.of(0, 10), null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStockAgeDays()).isEqualTo(3);
        assertThat(page.getContent().get(0).getProductSku()).isEqualTo("SKU001");
    }
}
