package com.zakat.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.zakat.enums.ZisType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateZakatPaymentRequest(
        @NotNull @Positive Integer jumlahJiwa,
        @NotBlank String alamat,
        @NotNull ZisType zakatType,
        UUID zakatQualityId,
        @Positive BigDecimal beratBerasKg,
        @Positive BigDecimal jumlahUang,
        @NotEmpty List<@NotBlank String> muzakkiNames
) {
}
