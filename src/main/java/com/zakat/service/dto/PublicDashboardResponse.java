package com.zakat.service.dto;

import com.zakat.enums.ZisType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PublicDashboardResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalTransaksi,
        BigDecimal totalUangMasuk,
        BigDecimal totalUangCash,
        BigDecimal totalUangTransfer,
        BigDecimal totalBerasKg,
        long totalJiwaFitrah,
        long totalJiwaFitrahBeras,
        long totalJiwaFitrahUang,
        List<ByType> byType,
        InstitutionProfileResponse institutionProfile,
        Instant generatedAt
) {
    public record ByType(
            ZisType zakatType,
            String zakatTypeLabel,
            BigDecimal totalUang,
            BigDecimal totalBerasKg
    ) {
    }
}
