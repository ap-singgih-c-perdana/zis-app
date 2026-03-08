package com.zakat.service;

import com.zakat.enums.ZakatType;
import com.zakat.repository.ZakatPaymentRepository;
import com.zakat.service.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Jakarta");

    private final ZakatPaymentRepository zakatPaymentRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate dan toDate wajib diisi");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate tidak boleh lebih kecil dari fromDate");
        }

        Instant fromInclusive = fromDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant toExclusive = toDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        ZakatPaymentRepository.DashboardTotalsRow totals = zakatPaymentRepository.dashboardTotals(
                fromInclusive,
                toExclusive,
                List.of(ZakatType.ZAKAT_FITRAH_BERAS, ZakatType.ZAKAT_FITRAH_UANG)
        );

        List<DashboardSummaryResponse.ByType> byType = zakatPaymentRepository.dashboardByType(fromInclusive, toExclusive).stream()
                .map(r -> new DashboardSummaryResponse.ByType(
                        r.getZakatType(),
                        r.getTransaksi(),
                        r.getTotalUang(),
                        r.getTotalBerasKg(),
                        r.getTotalJiwa()
                ))
                .toList();

        return new DashboardSummaryResponse(
                fromDate,
                toDate,
                totals.getTotalTransaksi(),
                totals.getTotalUangMasuk(),
                totals.getTotalBerasKg(),
                totals.getTotalJiwaFitrah(),
                byType
        );
    }
}

