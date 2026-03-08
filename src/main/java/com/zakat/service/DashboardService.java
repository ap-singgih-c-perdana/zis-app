package com.zakat.service;

import com.zakat.enums.ZisType;
import com.zakat.entity.InstitutionProfile;
import com.zakat.entity.ReceiptSequence;
import com.zakat.entity.ZakatPayment;
import com.zakat.repository.ReceiptSequenceRepository;
import com.zakat.repository.ZakatPaymentRepository;
import com.zakat.repository.ZakatQualityRepository;
import com.zakat.service.dto.DashboardSummaryResponse;
import com.zakat.service.dto.InstitutionProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final ZakatQualityRepository zakatQualityRepository;
    private final ReceiptSequenceRepository receiptSequenceRepository;
    private final InstitutionProfileService institutionProfileService;

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
                List.of(ZisType.ZAKAT_FITRAH_BERAS, ZisType.ZAKAT_FITRAH_UANG)
        );

        List<DashboardSummaryResponse.ByType> byType = zakatPaymentRepository.dashboardByType(fromInclusive, toExclusive).stream()
                .map(r -> new DashboardSummaryResponse.ByType(
                        r.getZakatType(),
                        r.getZakatType() == null ? null : r.getZakatType().getLabel(),
                        r.getTransaksi(),
                        r.getTotalUang(),
                        r.getTotalBerasKg(),
                        r.getTotalJiwa()
                ))
                .toList();

        InstitutionProfileResponse profileResponse = toProfileResponse(institutionProfileService.get());

        DashboardSummaryResponse.ReceiptInfo receiptInfo = buildReceiptInfo();

        List<DashboardSummaryResponse.ActiveQuality> activeQualities = List.of(
                new DashboardSummaryResponse.ActiveQuality(
                        ZisType.ZAKAT_FITRAH_UANG,
                        ZisType.ZAKAT_FITRAH_UANG.getLabel(),
                        zakatQualityRepository.countByZakatTypeAndActiveTrue(ZisType.ZAKAT_FITRAH_UANG)
                ),
                new DashboardSummaryResponse.ActiveQuality(
                        ZisType.ZAKAT_FITRAH_BERAS,
                        ZisType.ZAKAT_FITRAH_BERAS.getLabel(),
                        zakatQualityRepository.countByZakatTypeAndActiveTrue(ZisType.ZAKAT_FITRAH_BERAS)
                )
        );

        List<DashboardSummaryResponse.RecentPayment> recentPayments = zakatPaymentRepository.findRecent(
                        fromInclusive,
                        toExclusive,
                        PageRequest.of(0, 5)
                ).stream()
                .map(this::toRecentPayment)
                .toList();

        return new DashboardSummaryResponse(
                fromDate,
                toDate,
                totals.getTotalTransaksi(),
                totals.getTotalUangMasuk(),
                totals.getTotalBerasKg(),
                totals.getTotalJiwaFitrah(),
                byType,
                profileResponse,
                receiptInfo,
                activeQualities,
                recentPayments
        );
    }

    private DashboardSummaryResponse.RecentPayment toRecentPayment(ZakatPayment p) {
        int muzakkiCount = p.getMuzakkiList() == null ? 0 : p.getMuzakkiList().size();
        return new DashboardSummaryResponse.RecentPayment(
                p.getId(),
                p.getReceiptNumber(),
                p.getCreatedAt(),
                p.getAlamat(),
                p.getZakatType(),
                p.getZakatType() == null ? null : p.getZakatType().getLabel(),
                p.getJumlahJiwa(),
                p.getJumlahUang(),
                p.getBeratBerasKg(),
                muzakkiCount
        );
    }

    private DashboardSummaryResponse.ReceiptInfo buildReceiptInfo() {
        int year = LocalDate.now(DEFAULT_ZONE).getYear();
        long maxExisting = zakatPaymentRepository.maxReceiptSequenceForYear(year);
        long lastIssued = receiptSequenceRepository.findById(year)
                .map(ReceiptSequence::getLastIssued)
                .orElse(0L);
        long base = Math.max(maxExisting, lastIssued);
        String lastReceipt = base <= 0 ? null : String.format("KW/%d/%06d", year, base);
        String nextReceipt = String.format("KW/%d/%06d", year, base + 1);
        return new DashboardSummaryResponse.ReceiptInfo(year, base, lastReceipt, nextReceipt);
    }

    private static InstitutionProfileResponse toProfileResponse(InstitutionProfile profile) {
        if (profile == null) return null;
        return new InstitutionProfileResponse(
                profile.getId(),
                profile.getNamaInstansi(),
                profile.getKotaKabupaten(),
                profile.getAlamatLengkap(),
                profile.getNamaKetua(),
                profile.getNamaBendahara()
        );
    }
}
