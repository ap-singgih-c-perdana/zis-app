package com.zakat.service.dto;

import com.zakat.enums.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateZakatPaymentRequest(
        @PastOrPresent LocalDate paymentDate,
        @NotNull @Positive @Max(10) Integer jumlahJiwa,
        @NotBlank String alamat,
        @NotBlank String payerName,
        String payerPhone,
        @NotNull PaymentMethod paymentMethod,
        UUID zakatQualityId,
        @Positive BigDecimal beratBerasKg,
        @Positive BigDecimal jumlahUang,
        @Positive BigDecimal jumlahUangZakatMal,
        @Positive BigDecimal jumlahUangInfaqSedekah,
        @Positive BigDecimal jumlahUangFidiah,
        @NotNull @Size(max = 10) List<@NotBlank String> muzakkiNames
) {
}
