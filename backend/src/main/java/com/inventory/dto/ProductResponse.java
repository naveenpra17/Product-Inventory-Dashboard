package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String productSku;
    private String productName;
    private String category;
    private LocalDate purchaseDate;
    private BigDecimal unitPrice;
    private Integer quantity;
    private long stockAgeDays;
}
