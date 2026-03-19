package com.zakat.controller;

import com.zakat.service.DashboardService;
import com.zakat.service.dto.DashboardSummaryResponse;
import com.zakat.service.dto.PublicDashboardResponse;
import com.zakat.repository.ZakatPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/dashboard")
@RequiredArgsConstructor
public class PublicDashboardController {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Jakarta");

    private final DashboardService dashboardService;
    private final ZakatPaymentRepository zakatPaymentRepository;

    @GetMapping("/summary")
    public PublicDashboardResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate fromDate;
        LocalDate toDate;

        if (from == null && to == null) {
            Instant earliestPaymentAt = zakatPaymentRepository.minPaymentAt();
            fromDate = earliestPaymentAt == null ? today : LocalDate.ofInstant(earliestPaymentAt, DEFAULT_ZONE);
            toDate = today;
        } else {
            fromDate = from == null ? today : from;
            toDate = to == null ? fromDate : to;
        }

        try {
            DashboardSummaryResponse summary = dashboardService.summary(fromDate, toDate);
            Instant fromInclusive = fromDate.atStartOfDay(DEFAULT_ZONE).toInstant();
            Instant toExclusive = toDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
            Map<com.zakat.enums.ZisType, Long> fitrahJiwaByType = zakatPaymentRepository.fitrahJiwaBreakdown(
                            fromInclusive,
                            toExclusive,
                            java.util.List.of(com.zakat.enums.ZisType.ZAKAT_FITRAH_BERAS, com.zakat.enums.ZisType.ZAKAT_FITRAH_UANG)
                    ).stream()
                    .collect(Collectors.toMap(
                            ZakatPaymentRepository.FitrahJiwaBreakdownRow::getZakatType,
                            row -> row.getTotalJiwa() == null ? 0L : row.getTotalJiwa(),
                            Long::sum
                    ));
            return new PublicDashboardResponse(
                    summary.fromDate(),
                    summary.toDate(),
                    summary.totalTransaksi(),
                    summary.totalUangMasuk(),
                    summary.totalUangCash(),
                    summary.totalUangTransfer(),
                    summary.totalBerasKg(),
                    summary.totalJiwaFitrah(),
                    fitrahJiwaByType.getOrDefault(com.zakat.enums.ZisType.ZAKAT_FITRAH_BERAS, 0L),
                    fitrahJiwaByType.getOrDefault(com.zakat.enums.ZisType.ZAKAT_FITRAH_UANG, 0L),
                    summary.byType().stream()
                            .map(item -> new PublicDashboardResponse.ByType(
                                    item.zakatType(),
                                    item.zakatTypeLabel(),
                                    item.totalUang(),
                                    item.totalBerasKg()
                            ))
                            .toList(),
                    summary.institutionProfile(),
                    Instant.now()
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
