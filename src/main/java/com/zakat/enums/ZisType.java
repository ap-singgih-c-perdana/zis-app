package com.zakat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ZisType {
    ZAKAT_FITRAH_BERAS("Zakat Fitrah (Beras)"),
    ZAKAT_FITRAH_UANG("Zakat Fitrah (Uang)"),
    ZAKAT_MAL("Zakat Mal"),
    INFAQ_SEDEKAH("Infaq/Sedekah");

    private final String label;
}

