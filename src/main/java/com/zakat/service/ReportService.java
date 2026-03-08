package com.zakat.service;

import com.zakat.entity.InstitutionProfile;
import com.zakat.enums.ZakatType;
import com.zakat.repository.ZakatPaymentRepository;
import com.zakat.repository.MuzakkiPersonRepository;
import com.zakat.service.dto.InstitutionProfileResponse;
import com.zakat.service.dto.KwitansiReportResponse;
import com.zakat.service.dto.MuzakkiDetailReportResponse;
import com.zakat.service.dto.RekapZisReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Jakarta");

    private final ZakatPaymentRepository zakatPaymentRepository;
    private final MuzakkiPersonRepository muzakkiPersonRepository;
    private final InstitutionProfileService institutionProfileService;

    @Transactional(readOnly = true)
    public RekapZisReportResponse rekapZis(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate dan toDate wajib diisi");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate tidak boleh lebih kecil dari fromDate");
        }

        Instant fromInclusive = fromDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant toExclusive = toDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        List<ZakatPaymentRepository.RekapRow> rows = zakatPaymentRepository.rekapByType(fromInclusive, toExclusive);

        Map<ZakatType, BigDecimal> uangByType = new EnumMap<>(ZakatType.class);
        Map<ZakatType, BigDecimal> berasByType = new EnumMap<>(ZakatType.class);
        for (ZakatPaymentRepository.RekapRow row : rows) {
            uangByType.put(row.getZakatType(), defaultZero(row.getTotalUang()));
            berasByType.put(row.getZakatType(), defaultZero(row.getTotalBerasKg()));
        }

        BigDecimal zakatFitrahUang = uangByType.getOrDefault(ZakatType.ZAKAT_FITRAH_UANG, BigDecimal.ZERO);
        BigDecimal zakatMal = uangByType.getOrDefault(ZakatType.ZAKAT_MAL, BigDecimal.ZERO);
        BigDecimal infaqSedekah = uangByType.getOrDefault(ZakatType.INFAQ_SEDEKAH, BigDecimal.ZERO);
        BigDecimal totalUangMasuk = zakatFitrahUang.add(zakatMal).add(infaqSedekah);

        BigDecimal zakatFitrahBerasKg = berasByType.getOrDefault(ZakatType.ZAKAT_FITRAH_BERAS, BigDecimal.ZERO);

        long totalMuzakkiFitrahJiwa = zakatPaymentRepository.sumJiwaFitrah(
                fromInclusive,
                toExclusive,
                List.of(ZakatType.ZAKAT_FITRAH_BERAS, ZakatType.ZAKAT_FITRAH_UANG)
        );

        InstitutionProfile profile = institutionProfileService.get();
        InstitutionProfileResponse profileResponse = profile == null
                ? null
                : new InstitutionProfileResponse(
                profile.getId(),
                profile.getNamaInstansi(),
                profile.getKotaKabupaten(),
                profile.getAlamatLengkap(),
                profile.getNamaKetua(),
                profile.getNamaBendahara()
        );

        return new RekapZisReportResponse(
                fromDate,
                toDate,
                zakatFitrahUang,
                zakatFitrahBerasKg,
                zakatMal,
                infaqSedekah,
                totalUangMasuk,
                totalMuzakkiFitrahJiwa,
                profileResponse
        );
    }

    @Transactional(readOnly = true)
    public MuzakkiDetailReportResponse muzakkiDetail(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate dan toDate wajib diisi");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate tidak boleh lebih kecil dari fromDate");
        }

        Instant fromInclusive = fromDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant toExclusive = toDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        List<MuzakkiPersonRepository.MuzakkiReportRow> rows = muzakkiPersonRepository.findReportRows(fromInclusive, toExclusive);
        List<MuzakkiDetailReportResponse.Row> mapped = mapMuzakkiRows(rows);

        Map<UUID, MuzakkiPersonRepository.MuzakkiReportRow> firstRowByPaymentId = new LinkedHashMap<>();
        for (MuzakkiPersonRepository.MuzakkiReportRow row : rows) {
            firstRowByPaymentId.putIfAbsent(row.getPaymentId(), row);
        }

        BigDecimal totalNominalRp = firstRowByPaymentId.values().stream()
                .map(MuzakkiPersonRepository.MuzakkiReportRow::getJumlahUang)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBerasKg = firstRowByPaymentId.values().stream()
                .map(MuzakkiPersonRepository.MuzakkiReportRow::getBeratBerasKg)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalJiwa = firstRowByPaymentId.values().stream()
                .map(MuzakkiPersonRepository.MuzakkiReportRow::getJumlahJiwa)
                .filter(v -> v != null)
                .mapToLong(Integer::longValue)
                .sum();

        InstitutionProfile profile = institutionProfileService.get();
        InstitutionProfileResponse profileResponse = profile == null
                ? null
                : new InstitutionProfileResponse(
                profile.getId(),
                profile.getNamaInstansi(),
                profile.getKotaKabupaten(),
                profile.getAlamatLengkap(),
                profile.getNamaKetua(),
                profile.getNamaBendahara()
        );

        return new MuzakkiDetailReportResponse(
                fromDate,
                toDate,
                mapped,
                totalNominalRp,
                totalBerasKg,
                totalJiwa,
                profileResponse
        );
    }

    @Transactional(readOnly = true)
    public KwitansiReportResponse kwitansi(UUID paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId wajib diisi");
        }

        var payment = zakatPaymentRepository.findWithQualityById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment tidak ditemukan"));

        List<String> muzakkiNames = muzakkiPersonRepository.findNamesByPaymentId(paymentId);

        InstitutionProfile profile = institutionProfileService.get();
        InstitutionProfileResponse profileResponse = profile == null
                ? null
                : new InstitutionProfileResponse(
                profile.getId(),
                profile.getNamaInstansi(),
                profile.getKotaKabupaten(),
                profile.getAlamatLengkap(),
                profile.getNamaKetua(),
                profile.getNamaBendahara()
        );

        KwitansiReportResponse.ZakatQualitySummary qualitySummary = null;
        if (payment.getZakatQuality() != null) {
            qualitySummary = new KwitansiReportResponse.ZakatQualitySummary(
                    payment.getZakatQuality().getId(),
                    payment.getZakatQuality().getName(),
                    payment.getZakatQuality().getBeratPerJiwaKg(),
                    payment.getZakatQuality().getNominalPerJiwa()
            );
        }

        Instant createdAt = payment.getCreatedAt();
        LocalDate tanggal = createdAt == null ? null : LocalDate.ofInstant(createdAt, DEFAULT_ZONE);

        return new KwitansiReportResponse(
                payment.getId(),
                payment.getReceiptNumber(),
                createdAt,
                tanggal,
                payment.getZakatType(),
                zakatTypeLabel(payment.getZakatType()),
                payment.getJumlahJiwa(),
                payment.getAlamat(),
                payment.getJumlahUang(),
                payment.getBeratBerasKg(),
                qualitySummary,
                muzakkiNames.size(),
                muzakkiNames,
                profileResponse
        );
    }

    private static List<MuzakkiDetailReportResponse.Row> mapMuzakkiRows(
            List<MuzakkiPersonRepository.MuzakkiReportRow> rows
    ) {
        int[] counter = {0};
        return rows.stream()
                .map(r -> new MuzakkiDetailReportResponse.Row(
                        ++counter[0],
                        LocalDate.ofInstant(r.getCreatedAt(), DEFAULT_ZONE),
                        r.getNama(),
                        r.getZakatType(),
                        r.getJumlahUang(),
                        r.getBeratBerasKg(),
                        r.getJumlahJiwa()
                ))
                .toList();
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String zakatTypeLabel(ZakatType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case ZAKAT_FITRAH_UANG -> "Fitrah Uang";
            case ZAKAT_FITRAH_BERAS -> "Fitrah Beras";
            case ZAKAT_MAL -> "Zakat Mal";
            case INFAQ_SEDEKAH -> "Infaq/Sedekah";
        };
    }
}
