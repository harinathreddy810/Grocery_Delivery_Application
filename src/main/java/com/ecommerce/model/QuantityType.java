package com.ecommerce.model;

public enum QuantityType {
    KG("kg"),
    LITER("liter"),
    DOZEN("dozen"),
    PACK("pack"),
    PIECE("piece"),
    GRAM("gram"),
    ML("ml");

    private final String displayName;

    QuantityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}