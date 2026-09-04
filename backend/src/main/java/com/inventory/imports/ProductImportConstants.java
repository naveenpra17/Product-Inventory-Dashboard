package com.inventory.imports;

public final class ProductImportConstants {

    public static final String COL_PRODUCT_SKU = "Product SKU";
    public static final String COL_PRODUCT_NAME = "Product Name";
    public static final String COL_CATEGORY = "Category";
    public static final String COL_PURCHASE_DATE = "Purchase Date";
    public static final String COL_UNIT_PRICE = "Unit Price";
    public static final String COL_QUANTITY = "Quantity";

    public static final String[] REQUIRED_COLUMNS = {
            COL_PRODUCT_SKU,
            COL_PRODUCT_NAME,
            COL_CATEGORY,
            COL_PURCHASE_DATE,
            COL_UNIT_PRICE,
            COL_QUANTITY
    };

    private ProductImportConstants() {
    }
}
