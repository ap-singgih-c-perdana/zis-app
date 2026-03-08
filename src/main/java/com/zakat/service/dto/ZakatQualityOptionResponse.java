package com.zakat.service.dto;

import com.zakat.enums.ZakatType;

import java.math.BigDecimal;
import java.util.UUID;

public record ZakatQualityOptionResponse(
        UUID id,
        String name,
        ZakatType zakatType,
        BigDecimal beratPerJiwaKg,
        Long nominalPerJiwa
) {
}

