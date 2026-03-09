package com.zakat.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateZakatPaymentRequest(
        @NotBlank String alamat,
        String payerName,
        String payerPhone,
        UUID zakatQualityId,
        @Positive BigDecimal jumlahUang,
        @Positive BigDecimal jumlahUangZakatMal,
        @Positive BigDecimal jumlahUangInfaqSedekah,
        @Positive BigDecimal jumlahUangFidiah,
        @NotNull List<@NotBlank String> muzakkiNames
) {
}
