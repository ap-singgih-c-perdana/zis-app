package com.zakat.service.dto;

import com.zakat.enums.ZisType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ZakatPaymentListItemResponse(
        UUID id,
        String receiptNumber,
        Instant createdAt,
        ZisType zakatType,
        String zakatTypeLabel,
        BigDecimal beratBerasKg,
        BigDecimal jumlahUang,
        int muzakkiCount,
        String muzakkiPreview,
        String alamat,
        String payerName,
        String payerPhone,
        boolean canceled
) {
}
