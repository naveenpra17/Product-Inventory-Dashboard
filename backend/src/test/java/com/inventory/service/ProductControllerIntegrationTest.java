package com.inventory.service;

import com.inventory.IntegrationTest;
import com.inventory.entity.Product;
import com.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    @Test
    void importCsv_success() throws Exception {
        String csv = """
                Product SKU,Product Name,Category,Purchase Date,Unit Price,Quantity
                SKU001,Laptop,Electronics,2025-01-10,750.00,5
                SKU002,Mouse,Electronics,2025-02-15,25.50,20
                """;
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/products/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(2));
    }

    @Test
    void importCsv_duplicateInDatabase_returnsValidationErrors() throws Exception {
        productRepository.save(Product.builder()
                .productSku("SKU001")
                .productName("Existing")
                .category("Electronics")
                .purchaseDate(LocalDate.of(2025, 1, 10))
                .unitPrice(new BigDecimal("100.00"))
                .quantity(1)
                .build());

        String csv = """
                Product SKU,Product Name,Category,Purchase Date,Unit Price,Quantity
                SKU001,Laptop,Electronics,2025-01-10,750.00,5
                """;
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/products/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.errors[0].field", containsString("Product SKU")));
    }

    @Test
    void getProducts_supportsPaginationAndSorting() throws Exception {
        saveProduct("SKU001", LocalDate.of(2025, 1, 10));
        saveProduct("SKU002", LocalDate.of(2025, 3, 1));

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "purchaseDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getSummary_returnsAggregates() throws Exception {
        saveProduct("SKU001", LocalDate.of(2025, 1, 10), new BigDecimal("100.00"), 2);
        saveProduct("SKU002", LocalDate.of(2025, 2, 1), new BigDecimal("50.00"), 4);

        mockMvc.perform(get("/api/products/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(2))
                .andExpect(jsonPath("$.totalInventoryValue", notNullValue()))
                .andExpect(jsonPath("$.averageStockAgeDays").exists());
    }

    @Test
    void import_invalidExtension_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "products.txt", "text/plain",
                "data".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/products/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported file type")));
    }

    private void saveProduct(String sku, LocalDate purchaseDate) {
        saveProduct(sku, purchaseDate, new BigDecimal("100.00"), 1);
    }

    private void saveProduct(String sku, LocalDate purchaseDate, BigDecimal price, int quantity) {
        productRepository.save(Product.builder()
                .productSku(sku)
                .productName("Product " + sku)
                .category("Electronics")
                .purchaseDate(purchaseDate)
                .unitPrice(price)
                .quantity(quantity)
                .build());
    }
}
