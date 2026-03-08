package com.zakat.service.dto;

import com.zakat.enums.ZisType;

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
            ZisType zakatType,
            long transaksi,
            BigDecimal totalUang,
            BigDecimal totalBerasKg,
            long totalJiwa
    ) {
    }
}
