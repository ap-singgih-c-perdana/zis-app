package com.zakat.service.dto;

import com.zakat.enums.ZakatType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ZakatPaymentListItemResponse(
        UUID id,
        String receiptNumber,
        Instant createdAt,
        ZakatType zakatType,
        BigDecimal beratBerasKg,
        BigDecimal jumlahUang,
        int muzakkiCount,
        String muzakkiPreview,
        String alamat,
        boolean canceled
) {
}
