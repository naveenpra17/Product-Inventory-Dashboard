package com.inventory.imports.parser;

import com.inventory.dto.ValidationErrorDto;
import com.inventory.imports.model.RawProductRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelProductFileParserTest {

    private final ExcelProductFileParser parser = new ExcelProductFileParser();

    @Test
    void parsesValidExcelWithFormula() throws IOException {
        byte[] content = createWorkbookWithFormula();
        MockMultipartFile file = new MockMultipartFile("file", "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
        List<ValidationErrorDto> errors = new ArrayList<>();

        List<RawProductRow> rows = parser.parse(file, errors);

        assertThat(errors).isEmpty();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUnitPrice()).isEqualTo("150");
        assertThat(rows.get(0).getQuantity()).isEqualTo("10");
    }

    @Test
    void emptyFile_reportsStructuralError() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        List<ValidationErrorDto> errors = new ArrayList<>();

        List<RawProductRow> rows = parser.parse(file, errors);

        assertThat(rows).isEmpty();
        assertThat(errors).isNotEmpty();
    }

    @Test
    void missingColumns_reportsStructuralError() throws IOException {
        byte[] content = createWorkbookMissingColumns();
        MockMultipartFile file = new MockMultipartFile("file", "bad.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
        List<ValidationErrorDto> errors = new ArrayList<>();

        List<RawProductRow> rows = parser.parse(file, errors);

        assertThat(rows).isEmpty();
        assertThat(errors).anyMatch(e -> e.getMessage().contains("Required column is missing"));
    }

    private byte[] createWorkbookWithFormula() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Product SKU");
            header.createCell(1).setCellValue("Product Name");
            header.createCell(2).setCellValue("Category");
            header.createCell(3).setCellValue("Purchase Date");
            header.createCell(4).setCellValue("Unit Price");
            header.createCell(5).setCellValue("Quantity");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("SKU001");
            data.createCell(1).setCellValue("Laptop");
            data.createCell(2).setCellValue("Electronics");
            data.createCell(3).setCellValue("2025-01-10");
            data.createCell(4).setCellFormula("100+50");
            data.createCell(5).setCellFormula("5+5");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] createWorkbookMissingColumns() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Product SKU");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
