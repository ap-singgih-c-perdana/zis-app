package com.zakat.service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ZakatPaymentListItemResponse(
        UUID id,
        String receiptNumber,
        Instant createdAt,
        BigDecimal beratBerasKg,
        BigDecimal jumlahUang,
        BigDecimal jumlahUangZakatMal,
        BigDecimal jumlahUangInfaqSedekah,
        BigDecimal jumlahUangFidiah,
        int muzakkiCount,
        String muzakkiPreview,
        String alamat,
        String payerName,
        String payerPhone,
        boolean canceled
) {
}
