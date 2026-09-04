package com.inventory.imports.parser;

import com.inventory.dto.ValidationErrorDto;
import com.inventory.imports.ProductImportConstants;
import com.inventory.imports.model.RawProductRow;
import com.inventory.exception.InvalidFileException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CsvProductFileParser implements ProductFileParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    @Override
    public List<RawProductRow> parse(MultipartFile file, List<ValidationErrorDto> structuralErrors) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                structuralErrors.add(ValidationErrorDto.builder()
                        .field("File")
                        .message("CSV file is empty.")
                        .suggestion("Add a header row and at least one data row.")
                        .build());
                return List.of();
            }

            String[] header = allRows.get(0);
            if (header.length == 0 || Arrays.stream(header).allMatch(h -> h == null || h.trim().isEmpty())) {
                structuralErrors.add(ValidationErrorDto.builder()
                        .field("File")
                        .message("CSV file is missing a header row.")
                        .build());
                return List.of();
            }

            Map<String, Integer> columnIndex = buildColumnIndex(header, structuralErrors);
            if (!structuralErrors.isEmpty()) {
                return List.of();
            }

            List<RawProductRow> rows = new ArrayList<>();
            for (int i = 1; i < allRows.size(); i++) {
                String[] data = allRows.get(i);
                if (isRowBlank(data, columnIndex)) {
                    continue;
                }

                int rowNumber = i + 1;
                rows.add(RawProductRow.builder()
                        .rowNumber(rowNumber)
                        .productSku(getValue(data, columnIndex, ProductImportConstants.COL_PRODUCT_SKU))
                        .productName(getValue(data, columnIndex, ProductImportConstants.COL_PRODUCT_NAME))
                        .category(getValue(data, columnIndex, ProductImportConstants.COL_CATEGORY))
                        .purchaseDate(getValue(data, columnIndex, ProductImportConstants.COL_PURCHASE_DATE))
                        .unitPrice(getValue(data, columnIndex, ProductImportConstants.COL_UNIT_PRICE))
                        .quantity(getValue(data, columnIndex, ProductImportConstants.COL_QUANTITY))
                        .build());
            }

            if (rows.isEmpty() && structuralErrors.isEmpty()) {
                structuralErrors.add(ValidationErrorDto.builder()
                        .field("File")
                        .message("CSV file contains no data rows.")
                        .suggestion("Add at least one product row below the header.")
                        .build());
            }

            return rows;
        } catch (IOException | CsvException ex) {
            log.error("Failed to read CSV file", ex);
            throw new InvalidFileException("Unable to read CSV file: " + ex.getMessage());
        }
    }

    private Map<String, Integer> buildColumnIndex(String[] header, List<ValidationErrorDto> errors) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null && !header[i].trim().isEmpty()) {
                index.put(header[i].trim(), i);
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

    private boolean isRowBlank(String[] data, Map<String, Integer> columnIndex) {
        for (String column : ProductImportConstants.REQUIRED_COLUMNS) {
            String value = getValue(data, columnIndex, column);
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getValue(String[] data, Map<String, Integer> columnIndex, String column) {
        Integer idx = columnIndex.get(column);
        if (idx == null || idx >= data.length || data[idx] == null) {
            return null;
        }
        return data[idx].trim();
    }
}
