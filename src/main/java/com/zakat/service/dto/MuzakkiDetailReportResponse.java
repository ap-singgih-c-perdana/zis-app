package com.zakat.service.dto;

import com.zakat.enums.ZisType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MuzakkiDetailReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        List<Row> rows,
        BigDecimal totalNominalRp,
        BigDecimal totalBerasKg,
        long totalJiwa,
        InstitutionProfileResponse institutionProfile
) {
    public record Row(
            int no,
            LocalDate tanggal,
            String namaMuzakki,
            ZisType zakatType,
            String zakatTypeLabel,
            BigDecimal nominalRp,
            BigDecimal berasKg
    ) {
    }
}
