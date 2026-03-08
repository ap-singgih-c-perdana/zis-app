package com.zakat.service.dto;

import com.zakat.enums.ZakatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ZakatQualityUpsertRequest(
        @NotBlank String name,
        @NotNull ZakatType zakatType,
        @Positive BigDecimal beratPerJiwaKg,
        @Positive Long nominalPerJiwa,
        Boolean active
) {
}

