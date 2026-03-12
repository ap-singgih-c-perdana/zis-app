package com.zakat.service.dto;

import com.zakat.enums.ZisType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardSummaryResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalTransaksi,
        BigDecimal totalUangMasuk,
        BigDecimal totalUangCash,
        BigDecimal totalUangTransfer,
        BigDecimal totalBerasKg,
        long totalJiwaFitrah,
        List<ByType> byType,
        InstitutionProfileResponse institutionProfile,
        ReceiptInfo receiptInfo,
        List<ActiveQuality> activeQualities,
        List<RecentPayment> recentPayments
) {
    public record ByType(
            ZisType zakatType,
            String zakatTypeLabel,
            BigDecimal totalUang,
            BigDecimal totalBerasKg
    ) {
    }

    public record ReceiptInfo(
            int year,
            long lastIssued,
            String lastReceiptNumber,
            String nextReceiptNumber
    ) {
    }

    public record ActiveQuality(
            ZisType zakatType,
            String zakatTypeLabel,
            long activeCount
    ) {
    }

    public record RecentPayment(
            UUID id,
            String receiptNumber,
            Instant createdAt,
            String alamat,
            ZisType zakatType,
            String zakatTypeLabel,
            Integer jumlahJiwa,
            BigDecimal jumlahUang,
            BigDecimal beratBerasKg,
            int muzakkiCount
    ) {
    }
}
