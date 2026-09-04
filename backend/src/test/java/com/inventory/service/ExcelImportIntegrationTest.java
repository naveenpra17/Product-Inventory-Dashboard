package com.inventory.service;

import com.inventory.IntegrationTest;
import com.inventory.repository.ProductRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
class ExcelImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    @Test
    void importExcel_successWithFormulaEvaluation() throws Exception {
        byte[] content = createWorkbookWithFormula();
        MockMultipartFile file = new MockMultipartFile("file", "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        mockMvc.perform(multipart("/api/products/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount", is(1)));
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
            data.createCell(5).setCellFormula("3+2");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
