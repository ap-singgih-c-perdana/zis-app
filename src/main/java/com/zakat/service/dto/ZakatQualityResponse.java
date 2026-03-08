package com.zakat.service.dto;

import com.zakat.enums.ZisType;

import java.math.BigDecimal;
import java.util.UUID;

public record ZakatQualityResponse(
        UUID id,
        String name,
        ZisType zakatType,
        boolean active,
        BigDecimal beratPerJiwaKg,
        Long nominalPerJiwa
) {
}
