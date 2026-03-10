package com.zakat.enums;

public enum PaymentMethod {
    CASH("Cash"),
    TRANSFER("Transfer");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

