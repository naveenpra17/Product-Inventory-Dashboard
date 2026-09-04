package com.inventory.service;

import com.inventory.dto.ImportResponse;
import com.inventory.dto.ProductImportRow;
import com.inventory.dto.ValidationErrorDto;
import com.inventory.entity.Product;
import com.inventory.exception.FileValidationException;
import com.inventory.exception.InvalidFileException;
import com.inventory.imports.model.RawProductRow;
import com.inventory.imports.parser.ProductFileParser;
import com.inventory.imports.parser.ProductFileParserFactory;
import com.inventory.validation.ProductValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileImportService {

    private final ProductFileParserFactory parserFactory;
    private final ProductValidationService validationService;
    private final ProductService productService;

    @Transactional
    public ImportResponse importProducts(MultipartFile file) {
        validateFileMetadata(file);

        String filename = file.getOriginalFilename();
        ProductFileParser parser = parserFactory.getParser(filename);

        List<ValidationErrorDto> errors = new ArrayList<>();
        List<RawProductRow> rawRows = parser.parse(file, errors);

        if (!errors.isEmpty()) {
            throw new FileValidationException("Import validation failed", errors);
        }

        List<ProductImportRow> importRows = validationService.toImportRows(rawRows, errors);
        if (!errors.isEmpty()) {
            throw new FileValidationException("Import validation failed", errors);
        }

        List<Product> products = importRows.stream()
                .map(productService::toEntity)
                .toList();

        productService.saveAll(products);

        log.info("Successfully imported {} products from file {}", products.size(), filename);
        return ImportResponse.builder()
                .message("Successfully imported " + products.size() + " products.")
                .importedCount(products.size())
                .build();
    }

    private void validateFileMetadata(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileException("Uploaded file must have a valid filename.");
        }

        String lower = filename.toLowerCase();
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".csv")) {
            throw new InvalidFileException("Unsupported file type. Only .xlsx and .csv files are allowed.");
        }
    }
}
