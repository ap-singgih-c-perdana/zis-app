package com.zakat.service.dto;

import com.zakat.enums.ZisType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ZakatPaymentResponse(
        UUID id,
        String receiptNumber,
        boolean canceled,
        Instant createdAt,
        ZisType zakatType,
        String zakatTypeLabel,
        Integer jumlahJiwa,
        String alamat,
        String payerName,
        String payerPhone,
        BigDecimal beratBerasKg,
        BigDecimal jumlahUang,
        ZakatQualitySummary zakatQuality,
        List<String> muzakkiNames
) {
    public record ZakatQualitySummary(
            UUID id,
            String name,
            BigDecimal beratPerJiwaKg
    ) {
    }
}
