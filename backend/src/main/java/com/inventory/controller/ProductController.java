package com.inventory.controller;

import com.inventory.dto.ImportResponse;
import com.inventory.dto.ProductResponse;
import com.inventory.dto.ProductSummaryResponse;
import com.inventory.service.FileImportService;
import com.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final FileImportService fileImportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResponse> importProducts(@RequestParam("file") MultipartFile file) {
        ImportResponse response = fileImportService.importProducts(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @PageableDefault(size = 10, sort = "purchaseDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.getProducts(pageable, search));
    }

    @GetMapping("/summary")
    public ResponseEntity<ProductSummaryResponse> getSummary() {
        return ResponseEntity.ok(productService.getSummary());
    }
}
