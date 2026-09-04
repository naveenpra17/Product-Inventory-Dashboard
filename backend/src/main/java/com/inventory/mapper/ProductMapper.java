package com.inventory.mapper;

import com.inventory.dto.ProductImportRow;
import com.inventory.dto.ProductResponse;
import com.inventory.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product, long stockAgeDays) {
        return ProductResponse.builder()
                .id(product.getId())
                .productSku(product.getProductSku())
                .productName(product.getProductName())
                .category(product.getCategory())
                .purchaseDate(product.getPurchaseDate())
                .unitPrice(product.getUnitPrice())
                .quantity(product.getQuantity())
                .stockAgeDays(stockAgeDays)
                .build();
    }

    public Product toEntity(ProductImportRow row) {
        return Product.builder()
                .productSku(row.getProductSku())
                .productName(row.getProductName())
                .category(row.getCategory())
                .purchaseDate(row.getPurchaseDate())
                .unitPrice(row.getUnitPrice())
                .quantity(row.getQuantity())
                .build();
    }
}
