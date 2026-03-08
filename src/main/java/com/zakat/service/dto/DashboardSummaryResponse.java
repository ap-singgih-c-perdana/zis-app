package com.zakat.service.dto;

import com.zakat.enums.ZakatType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalTransaksi,
        BigDecimal totalUangMasuk,
        BigDecimal totalBerasKg,
        long totalJiwaFitrah,
        List<ByType> byType
) {
    public record ByType(
            ZakatType zakatType,
            long transaksi,
            BigDecimal totalUang,
            BigDecimal totalBerasKg,
            long totalJiwa
    ) {
    }
}

