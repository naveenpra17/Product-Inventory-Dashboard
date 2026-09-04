package com.inventory.imports.parser;

import com.inventory.dto.ValidationErrorDto;
import com.inventory.imports.model.RawProductRow;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductFileParser {

    boolean supports(String filename);

    List<RawProductRow> parse(MultipartFile file, List<ValidationErrorDto> structuralErrors);
}
