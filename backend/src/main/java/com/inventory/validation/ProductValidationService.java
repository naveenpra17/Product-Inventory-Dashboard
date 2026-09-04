package com.inventory.validation;

import com.inventory.dto.ProductImportRow;
import com.inventory.dto.ValidationErrorDto;
import com.inventory.imports.ProductImportConstants;
import com.inventory.imports.model.RawProductRow;
import com.inventory.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductValidationService {

    private final ProductRepository productRepository;
    private final DateTimeFormatter dateFormatter;

    public ProductValidationService(
            ProductRepository productRepository,
            @Value("${app.import.date-format:uuuu-MM-dd}") String dateFormat) {
        this.productRepository = productRepository;
        this.dateFormatter = DateTimeFormatter.ofPattern(dateFormat)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    public List<ProductImportRow> toImportRows(List<RawProductRow> rawRows, List<ValidationErrorDto> errors) {
        List<ProductImportRow> result = new ArrayList<>();
        Set<String> fileKeys = new HashSet<>();

        for (RawProductRow raw : rawRows) {
            String sku = validateRequiredText(raw.getRowNumber(), ProductImportConstants.COL_PRODUCT_SKU,
                    raw.getProductSku(), errors);
            String name = validateRequiredText(raw.getRowNumber(), ProductImportConstants.COL_PRODUCT_NAME,
                    raw.getProductName(), errors);
            String category = validateRequiredText(raw.getRowNumber(), ProductImportConstants.COL_CATEGORY,
                    raw.getCategory(), errors);
            LocalDate purchaseDate = validatePurchaseDate(raw.getRowNumber(), raw.getPurchaseDate(), errors);
            BigDecimal unitPrice = validateUnitPrice(raw.getRowNumber(), raw.getUnitPrice(), errors);
            Integer quantity = validateQuantity(raw.getRowNumber(), raw.getQuantity(), errors);

            if (sku == null || name == null || category == null
                    || purchaseDate == null || unitPrice == null || quantity == null) {
                continue;
            }

            String key = buildKey(sku, purchaseDate);
            if (!fileKeys.add(key)) {
                errors.add(ValidationErrorDto.builder()
                        .row(raw.getRowNumber())
                        .field("Product SKU + Purchase Date")
                        .message("Duplicate combination within the uploaded file.")
                        .suggestion("Ensure each SKU and Purchase Date pair is unique in the file.")
                        .build());
                continue;
            }

            result.add(ProductImportRow.builder()
                    .rowNumber(raw.getRowNumber())
                    .productSku(sku)
                    .productName(name)
                    .category(category)
                    .purchaseDate(purchaseDate)
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .build());
        }

        if (!result.isEmpty()) {
            validateAgainstDatabase(result, errors);
        }

        return result;
    }

    private void validateAgainstDatabase(List<ProductImportRow> rows, List<ValidationErrorDto> errors) {
        Set<String> keys = rows.stream()
                .map(row -> buildKey(row.getProductSku(), row.getPurchaseDate()))
                .collect(Collectors.toSet());

        Set<String> existingKeys = productRepository.findExistingSkuDateKeys(keys);

        for (ProductImportRow row : rows) {
            String key = buildKey(row.getProductSku(), row.getPurchaseDate());
            if (existingKeys.contains(key)) {
                errors.add(ValidationErrorDto.builder()
                        .row(row.getRowNumber())
                        .field("Product SKU + Purchase Date")
                        .message("Product SKU + Purchase Date already exists in the database.")
                        .suggestion("Remove or update the duplicate record before importing.")
                        .build());
            }
        }
    }

    private String validateRequiredText(int rowNumber, String field, String value,
                                        List<ValidationErrorDto> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(field)
                    .message(field + " cannot be blank.")
                    .suggestion("Provide a value for " + field + ".")
                    .build());
            return null;
        }
        return value.trim();
    }

    private LocalDate validatePurchaseDate(int rowNumber, String value, List<ValidationErrorDto> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(ProductImportConstants.COL_PURCHASE_DATE)
                    .message("Purchase Date cannot be blank.")
                    .suggestion("Use format uuuu-MM-dd (e.g. 2025-08-15).")
                    .build());
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), dateFormatter);
        } catch (DateTimeParseException ex) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(ProductImportConstants.COL_PURCHASE_DATE)
                    .message("Invalid date format.")
                    .suggestion("Use format uuuu-MM-dd (e.g. 2025-08-15).")
                    .build());
            return null;
        }
    }

    private BigDecimal validateUnitPrice(int rowNumber, String value, List<ValidationErrorDto> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(ProductImportConstants.COL_UNIT_PRICE)
                    .message("Unit Price cannot be blank.")
                    .suggestion("Provide a positive numeric value.")
                    .build());
            return null;
        }
        try {
            String normalized = value.trim().replace("$", "").replace(",", "");
            BigDecimal price = new BigDecimal(normalized);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(ValidationErrorDto.builder()
                        .row(rowNumber)
                        .field(ProductImportConstants.COL_UNIT_PRICE)
                        .message("Unit Price must be a positive number.")
                        .suggestion("Enter a value greater than zero.")
                        .build());
                return null;
            }
            return price.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(ProductImportConstants.COL_UNIT_PRICE)
                    .message("Invalid currency value.")
                    .suggestion("Provide a valid positive number (e.g. 750.00).")
                    .build());
            return null;
        }
    }

    private Integer validateQuantity(int rowNumber, String value, List<ValidationErrorDto> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(ProductImportConstants.COL_QUANTITY)
                    .message("Quantity cannot be blank.")
                    .suggestion("Provide a non-negative whole number.")
                    .build());
            return null;
        }
        try {
            String normalized = value.trim().replace(",", "");
            if (normalized.contains(".")) {
                BigDecimal decimal = new BigDecimal(normalized);
                if (decimal.scale() > 0 && decimal.stripTrailingZeros().scale() > 0) {
                    errors.add(ValidationErrorDto.builder()
                            .row(rowNumber)
                            .field(ProductImportConstants.COL_QUANTITY)
                            .message("Quantity must be a whole number.")
                            .suggestion("Provide a non-negative integer value.")
                            .build());
                    return null;
                }
            }
            int quantity = Integer.parseInt(normalized.split("\\.")[0]);
            if (quantity < 0) {
                errors.add(ValidationErrorDto.builder()
                        .row(rowNumber)
                        .field(ProductImportConstants.COL_QUANTITY)
                        .message("Quantity must be non-negative.")
                        .suggestion("Provide zero or a positive integer.")
                        .build());
                return null;
            }
            return quantity;
        } catch (NumberFormatException ex) {
            errors.add(ValidationErrorDto.builder()
                    .row(rowNumber)
                    .field(ProductImportConstants.COL_QUANTITY)
                    .message("Invalid quantity value.")
                    .suggestion("Provide a non-negative integer.")
                    .build());
            return null;
        }
    }

    private String buildKey(String sku, LocalDate purchaseDate) {
        return sku + "|" + purchaseDate;
    }
}
