package com.inventory.imports.parser;

import com.inventory.dto.ValidationErrorDto;
import com.inventory.imports.ProductImportConstants;
import com.inventory.imports.model.RawProductRow;
import com.inventory.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ExcelProductFileParser implements ProductFileParser {

    private static final DateTimeFormatter EXCEL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".xlsx");
    }

    @Override
    public List<RawProductRow> parse(MultipartFile file, List<ValidationErrorDto> structuralErrors) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                structuralErrors.add(ValidationErrorDto.builder()
                        .field("File")
                        .message("Excel file contains no sheets.")
                        .suggestion("Provide a valid Excel workbook with a data sheet.")
                        .build());
                return List.of();
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                structuralErrors.add(ValidationErrorDto.builder()
                        .field("File")
                        .message("Excel file is empty.")
                        .suggestion("Add a header row and at least one data row.")
                        .build());
                return List.of();
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                structuralErrors.add(ValidationErrorDto.builder()
                        .field("File")
                        .message("Excel file is missing a header row.")
                        .build());
                return List.of();
            }

            Map<String, Integer> columnIndex = buildColumnIndex(headerRow, structuralErrors);
            if (!structuralErrors.isEmpty()) {
                return List.of();
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            List<RawProductRow> rows = new ArrayList<>();

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowBlank(row, columnIndex)) {
                    continue;
                }

                int rowNumber = i + 1;
                rows.add(RawProductRow.builder()
                        .rowNumber(rowNumber)
                        .productSku(extractCellValue(row, columnIndex.get(ProductImportConstants.COL_PRODUCT_SKU),
                                ProductImportConstants.COL_PRODUCT_SKU, rowNumber, structuralErrors, evaluator, formatter))
                        .productName(extractCellValue(row, columnIndex.get(ProductImportConstants.COL_PRODUCT_NAME),
                                ProductImportConstants.COL_PRODUCT_NAME, rowNumber, structuralErrors, evaluator, formatter))
                        .category(extractCellValue(row, columnIndex.get(ProductImportConstants.COL_CATEGORY),
                                ProductImportConstants.COL_CATEGORY, rowNumber, structuralErrors, evaluator, formatter))
                        .purchaseDate(extractDateValue(row, columnIndex.get(ProductImportConstants.COL_PURCHASE_DATE),
                                rowNumber, structuralErrors, evaluator, formatter))
                        .unitPrice(extractCellValue(row, columnIndex.get(ProductImportConstants.COL_UNIT_PRICE),
                                ProductImportConstants.COL_UNIT_PRICE, rowNumber, structuralErrors, evaluator, formatter))
                        .quantity(extractCellValue(row, columnIndex.get(ProductImportConstants.COL_QUANTITY),
                                ProductImportConstants.COL_QUANTITY, rowNumber, structuralErrors, evaluator, formatter))
                        .build());
            }

            if (rows.isEmpty() && structuralErrors.isEmpty()) {
                structuralErrors.add(ValidationErrorDto.builder()
                        .field("File")
                        .message("Excel file contains no data rows.")
                        .suggestion("Add at least one product row below the header.")
                        .build());
            }

            return rows;
        } catch (org.apache.poi.EmptyFileException ex) {
            structuralErrors.add(ValidationErrorDto.builder()
                    .field("File")
                    .message("Excel file is empty.")
                    .suggestion("Provide a non-empty .xlsx file with product data.")
                    .build());
            return List.of();
        } catch (IOException ex) {
            log.error("Failed to read Excel file", ex);
            throw new InvalidFileException("Unable to read Excel file: " + ex.getMessage());
        }
    }

    private Map<String, Integer> buildColumnIndex(Row headerRow, List<ValidationErrorDto> errors) {
        Map<String, Integer> index = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue();
            if (header != null) {
                index.put(header.trim(), cell.getColumnIndex());
            }
        }

        for (String required : ProductImportConstants.REQUIRED_COLUMNS) {
            if (!index.containsKey(required)) {
                errors.add(ValidationErrorDto.builder()
                        .field(required)
                        .message("Required column is missing.")
                        .suggestion("Add column: " + required)
                        .build());
            }
        }
        return index;
    }

    private boolean isRowBlank(Row row, Map<String, Integer> columnIndex) {
        for (String column : ProductImportConstants.REQUIRED_COLUMNS) {
            Integer colIdx = columnIndex.get(column);
            if (colIdx == null) {
                continue;
            }
            Cell cell = row.getCell(colIdx);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = cell.toString();
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String extractCellValue(Row row, Integer colIndex, String fieldName, int rowNumber,
                                    List<ValidationErrorDto> errors, FormulaEvaluator evaluator,
                                    DataFormatter formatter) {
        if (colIndex == null) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                evaluator.evaluateFormulaCell(cell);
            }
            return formatter.formatCellValue(cell, evaluator).trim();
        } catch (Exception ex) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(fieldName)
                    .message("Unable to evaluate formula or read cell value.")
                    .suggestion("Ensure the formula is valid and produces a usable value.")
                    .build());
            return null;
        }
    }

    private String extractDateValue(Row row, Integer colIndex, int rowNumber,
                                    List<ValidationErrorDto> errors, FormulaEvaluator evaluator,
                                    DataFormatter formatter) {
        if (colIndex == null) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                evaluator.evaluateFormulaCell(cell);
            }

            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                return EXCEL_DATE_FORMAT.format(date);
            }

            if (cell.getCellType() == CellType.NUMERIC) {
                double numericValue = cell.getNumericCellValue();
                if (DateUtil.isValidExcelDate(numericValue)) {
                    LocalDate date = DateUtil.getLocalDateTime(numericValue).toLocalDate();
                    return EXCEL_DATE_FORMAT.format(date);
                }
            }

            return formatter.formatCellValue(cell, evaluator).trim();
        } catch (Exception ex) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(ProductImportConstants.COL_PURCHASE_DATE)
                    .message("Unable to evaluate date formula or read date cell.")
                    .suggestion("Use yyyy-MM-dd format or a valid Excel date.")
                    .build());
            return null;
        }
    }
}
