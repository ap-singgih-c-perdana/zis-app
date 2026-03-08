package com.zakat.service.dto;

import com.zakat.enums.ZakatType;

import java.math.BigDecimal;
import java.util.UUID;

public record ZakatQualityResponse(
        UUID id,
        String name,
        ZakatType zakatType,
        boolean active,
        BigDecimal beratPerJiwaKg,
        Long nominalPerJiwa
) {
}

