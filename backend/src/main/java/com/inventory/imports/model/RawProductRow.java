package com.inventory.imports.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawProductRow {

    private int rowNumber;
    private String productSku;
    private String productName;
    private String category;
    private String purchaseDate;
    private String unitPrice;
    private String quantity;
}
