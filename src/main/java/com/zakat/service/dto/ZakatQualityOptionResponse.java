package com.zakat.service.dto;

import com.zakat.enums.ZisType;

import java.math.BigDecimal;
import java.util.UUID;

public record ZakatQualityOptionResponse(
        UUID id,
        String name,
        ZisType zakatType,
        BigDecimal beratPerJiwaKg,
        Long nominalPerJiwa
) {
}
