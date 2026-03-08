package com.zakat.service.dto;

import com.zakat.enums.ZakatType;

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
            ZakatType zakatType,
            BigDecimal nominalRp,
            BigDecimal berasKg,
            Integer jiwa
    ) {
    }
}
