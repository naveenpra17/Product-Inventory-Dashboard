package com.inventory.validation;

import com.inventory.dto.ProductImportRow;
import com.inventory.dto.ValidationErrorDto;
import com.inventory.imports.model.RawProductRow;
import com.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductValidationServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ProductValidationService(productRepository, "uuuu-MM-dd");
        when(productRepository.findExistingSkuDateKeys(any())).thenReturn(Collections.emptySet());
    }

    @Test
    void validRow_parsesSuccessfully() {
        List<ValidationErrorDto> errors = new ArrayList<>();
        List<ProductImportRow> rows = validationService.toImportRows(List.of(validRawRow(2)), errors);

        assertThat(errors).isEmpty();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getProductSku()).isEqualTo("SKU001");
    }

    @Test
    void blankSku_reportsError() {
        RawProductRow row = validRawRow(2);
        row.setProductSku("  ");
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(row), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Product SKU"));
    }

    @Test
    void blankProductName_reportsError() {
        RawProductRow row = validRawRow(2);
        row.setProductName("");
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(row), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Product Name"));
    }

    @Test
    void blankCategory_reportsError() {
        RawProductRow row = validRawRow(2);
        row.setCategory("");
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(row), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Category"));
    }

    @Test
    void invalidPurchaseDate_reportsError() {
        RawProductRow row = validRawRow(2);
        row.setPurchaseDate("15-08-2025");
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(row), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Purchase Date")
                && e.getMessage().contains("Invalid date"));
    }

    @Test
    void invalidUnitPrice_reportsError() {
        RawProductRow row = validRawRow(2);
        row.setUnitPrice("abc");
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(row), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Unit Price"));
    }

    @Test
    void negativeUnitPrice_reportsError() {
        RawProductRow row = validRawRow(2);
        row.setUnitPrice("-10");
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(row), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Unit Price"));
    }

    @Test
    void invalidQuantity_reportsError() {
        RawProductRow row = validRawRow(2);
        row.setQuantity("2.5");
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(row), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Quantity"));
    }

    @Test
    void negativeQuantity_reportsError() {
        RawProductRow row = validRawRow(2);
        row.setQuantity("-1");
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(row), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Quantity"));
    }

    @Test
    void duplicateWithinFile_reportsError() {
        List<ValidationErrorDto> errors = new ArrayList<>();
        validationService.toImportRows(List.of(validRawRow(2), validRawRow(3)), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Product SKU + Purchase Date")
                && e.getMessage().contains("within the uploaded file"));
    }

    @Test
    void duplicateInDatabase_reportsError() {
        when(productRepository.findExistingSkuDateKeys(any()))
                .thenReturn(Set.of("SKU001|2025-01-10"));
        List<ValidationErrorDto> errors = new ArrayList<>();

        validationService.toImportRows(List.of(validRawRow(2)), errors);

        assertThat(errors).anyMatch(e -> e.getField().equals("Product SKU + Purchase Date")
                && e.getMessage().contains("database"));
    }

    private RawProductRow validRawRow(int rowNumber) {
        return RawProductRow.builder()
                .rowNumber(rowNumber)
                .productSku("SKU001")
                .productName("Laptop")
                .category("Electronics")
                .purchaseDate("2025-01-10")
                .unitPrice("750.00")
                .quantity("5")
                .build();
    }
}
