package com.zakat.service.dto;

import com.zakat.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateZakatPaymentRequest(
        @PastOrPresent LocalDate paymentDate,
        @NotBlank String alamat,
        String payerName,
        String payerPhone,
        String receivedByName,
        @NotNull PaymentMethod paymentMethod,
        UUID zakatQualityId,
        @Positive BigDecimal jumlahUang,
        @Positive BigDecimal jumlahUangZakatMal,
        @Positive BigDecimal jumlahUangInfaqSedekah,
        @Positive BigDecimal jumlahUangFidiah,
        @NotNull @Size(max = 10) List<@NotBlank String> muzakkiNames
) {
}
