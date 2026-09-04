package com.inventory.imports.parser;

import com.inventory.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class ProductFileParserFactory {

    private final List<ProductFileParser> parsers;

    public ProductFileParserFactory(List<ProductFileParser> parsers) {
        this.parsers = parsers;
    }

    public ProductFileParser getParser(String filename) {
        return parsers.stream()
                .filter(parser -> parser.supports(filename))
                .findFirst()
                .orElseThrow(() -> new InvalidFileException(
                        "Unsupported file type. Only .xlsx and .csv files are allowed."));
    }
}
