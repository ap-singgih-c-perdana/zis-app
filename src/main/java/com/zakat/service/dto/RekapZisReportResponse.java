package com.zakat.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RekapZisReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal zakatFitrahUang,
        BigDecimal zakatFitrahBerasKg,
        BigDecimal zakatMal,
        BigDecimal infaqSedekah,
        BigDecimal totalUangMasuk,
        long totalMuzakkiFitrahJiwa,
        InstitutionProfileResponse institutionProfile
) {
}

