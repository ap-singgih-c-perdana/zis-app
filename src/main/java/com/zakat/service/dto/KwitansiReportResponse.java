package com.zakat.service.dto;

import com.zakat.enums.ZakatType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record KwitansiReportResponse(
        UUID paymentId,
        String receiptNumber,
        Instant createdAt,
        LocalDate tanggal,
        ZakatType zakatType,
        String zakatTypeLabel,
        Integer jumlahJiwa,
        String alamat,
        BigDecimal nominalRp,
        BigDecimal berasKg,
        ZakatQualitySummary zakatQuality,
        int muzakkiCount,
        List<String> muzakkiNames,
        InstitutionProfileResponse institutionProfile
) {
    public record ZakatQualitySummary(
            UUID id,
            String name,
            BigDecimal beratPerJiwaKg,
            Long nominalPerJiwa
    ) {
    }
}

