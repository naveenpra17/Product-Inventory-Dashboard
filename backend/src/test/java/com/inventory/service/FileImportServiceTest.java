package com.inventory.service;

import com.inventory.exception.InvalidFileException;
import com.inventory.imports.parser.ProductFileParserFactory;
import com.inventory.repository.ProductRepository;
import com.inventory.validation.ProductValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileImportServiceTest {

    @Mock
    private ProductFileParserFactory parserFactory;

    @Mock
    private ProductValidationService validationService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private FileImportService fileImportService;

    @Test
    void emptyFile_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> fileImportService.importProducts(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void invalidExtension_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> fileImportService.importProducts(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Unsupported file type");
    }
}
