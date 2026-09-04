package com.inventory.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SampleFileGenerator {

    public static void main(String[] args) throws IOException {
        Path outputDir = Path.of("..", "sample-data").normalize().toAbsolutePath();
        Files.createDirectories(outputDir);
        writeValidWorkbook(outputDir.resolve("sample-products.xlsx"));
        writeInvalidWorkbook(outputDir.resolve("sample-products-invalid.xlsx"));
        System.out.println("Sample files written to " + outputDir);
    }

    private static void writeValidWorkbook(Path path) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");
            writeHeader(sheet.createRow(0));

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("SKU001");
            row1.createCell(1).setCellValue("Laptop");
            row1.createCell(2).setCellValue("Electronics");
            row1.createCell(3).setCellValue("2025-01-10");
            row1.createCell(4).setCellFormula("100+50");
            row1.createCell(5).setCellValue(5);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("SKU002");
            row2.createCell(1).setCellValue("Wireless Mouse");
            row2.createCell(2).setCellValue("Electronics");
            row2.createCell(3).setCellValue("2025-02-15");
            row2.createCell(4).setCellValue(25.50);
            row2.createCell(5).setCellFormula("10+10");

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("SKU003");
            row3.createCell(1).setCellValue("Office Chair");
            row3.createCell(2).setCellValue("Furniture");
            row3.createCell(3).setCellValue("2025-03-01");
            row3.createCell(4).setCellValue(199.99);
            row3.createCell(5).setCellValue(8);

            try (OutputStream out = Files.newOutputStream(path)) {
                workbook.write(out);
            }
        }
    }

    private static void writeInvalidWorkbook(Path path) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");
            writeHeader(sheet.createRow(0));

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("SKU001");
            row1.createCell(1).setCellValue("Laptop");
            row1.createCell(2).setCellValue("Electronics");
            row1.createCell(3).setCellValue("2025-01-10");
            row1.createCell(4).setCellValue(750.00);
            row1.createCell(5).setCellValue(5);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("SKU001");
            row2.createCell(1).setCellValue("Laptop Duplicate");
            row2.createCell(2).setCellValue("Electronics");
            row2.createCell(3).setCellValue("2025-01-10");
            row2.createCell(4).setCellValue(800.00);
            row2.createCell(5).setCellValue(2);

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("SKU006");
            row3.createCell(1).setCellValue("Desk Lamp");
            row3.createCell(2).setCellValue("Home");
            row3.createCell(3).setCellValue("not-a-date");
            row3.createCell(4).setCellFormula("1/0");
            row3.createCell(5).setCellValue(-3);

            try (OutputStream out = Files.newOutputStream(path)) {
                workbook.write(out);
            }
        }
    }

    private static void writeHeader(Row header) {
        header.createCell(0).setCellValue("Product SKU");
        header.createCell(1).setCellValue("Product Name");
        header.createCell(2).setCellValue("Category");
        header.createCell(3).setCellValue("Purchase Date");
        header.createCell(4).setCellValue("Unit Price");
        header.createCell(5).setCellValue("Quantity");
    }

    private SampleFileGenerator() {
    }
}
