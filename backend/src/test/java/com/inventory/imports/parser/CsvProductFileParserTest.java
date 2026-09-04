package com.inventory.imports.parser;

import com.inventory.dto.ValidationErrorDto;
import com.inventory.imports.model.RawProductRow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvProductFileParserTest {

    private final CsvProductFileParser parser = new CsvProductFileParser();

    @Test
    void parsesValidCsv() {
        String csv = """
                Product SKU,Product Name,Category,Purchase Date,Unit Price,Quantity
                SKU001,Laptop,Electronics,2025-01-10,750.00,5
                """;
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        List<ValidationErrorDto> errors = new ArrayList<>();

        List<RawProductRow> rows = parser.parse(file, errors);

        assertThat(errors).isEmpty();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getProductSku()).isEqualTo("SKU001");
    }

    @Test
    void emptyCsv_reportsStructuralError() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        List<ValidationErrorDto> errors = new ArrayList<>();

        List<RawProductRow> rows = parser.parse(file, errors);

        assertThat(rows).isEmpty();
        assertThat(errors).isNotEmpty();
    }

    @Test
    void supportsCsvExtension() {
        assertThat(parser.supports("sample.csv")).isTrue();
        assertThat(parser.supports("sample.xlsx")).isFalse();
    }
}
