package com.zakat.service;

import com.zakat.entity.InstitutionProfile;
import com.zakat.enums.ZisType;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        Map<ZisType, BigDecimal> uangByType = new EnumMap<>(ZisType.class);
        Map<ZisType, BigDecimal> berasByType = new EnumMap<>(ZisType.class);
        for (ZakatPaymentRepository.RekapRow row : rows) {
            uangByType.put(row.getZakatType(), defaultZero(row.getTotalUang()));
            berasByType.put(row.getZakatType(), defaultZero(row.getTotalBerasKg()));
        }

        BigDecimal zakatFitrahUang = uangByType.getOrDefault(ZisType.ZAKAT_FITRAH_UANG, BigDecimal.ZERO);
        BigDecimal zakatMal = uangByType.getOrDefault(ZisType.ZAKAT_MAL, BigDecimal.ZERO);
        BigDecimal infaqSedekah = uangByType.getOrDefault(ZisType.INFAQ_SEDEKAH, BigDecimal.ZERO);
        BigDecimal totalUangMasuk = zakatFitrahUang.add(zakatMal).add(infaqSedekah);

        BigDecimal zakatFitrahBerasKg = berasByType.getOrDefault(ZisType.ZAKAT_FITRAH_BERAS, BigDecimal.ZERO);

        long totalMuzakkiFitrahJiwa = zakatPaymentRepository.sumJiwaFitrah(
                fromInclusive,
                toExclusive,
                List.of(ZisType.ZAKAT_FITRAH_BERAS, ZisType.ZAKAT_FITRAH_UANG)
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
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBerasKg = firstRowByPaymentId.values().stream()
                .map(MuzakkiPersonRepository.MuzakkiReportRow::getBeratBerasKg)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalJiwa = firstRowByPaymentId.values().stream()
                .map(MuzakkiPersonRepository.MuzakkiReportRow::getJumlahJiwa)
                .filter(Objects::nonNull)
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
                payment.getZakatType() == null ? null : payment.getZakatType().getLabel(),
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

    public String muzakkiDetailCsv(MuzakkiDetailReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Periode,").append(csv(report.fromDate())).append(",").append(csv(report.toDate())).append("\n");

        if (report.institutionProfile() != null) {
            var p = report.institutionProfile();
            sb.append("Instansi,").append(csv(p.namaInstansi())).append("\n");
            sb.append("Kota/Kabupaten,").append(csv(p.kotaKabupaten())).append("\n");
            sb.append("Alamat,").append(csv(p.alamatLengkap())).append("\n");
            sb.append("\n");
        }

        sb.append("No,Tanggal,Nama Muzakki,Jenis,Nominal (Rp),Beras (Kg)\n");
        for (MuzakkiDetailReportResponse.Row r : report.rows()) {
            sb.append(r.no()).append(",");
            sb.append(csv(r.tanggal())).append(",");
            sb.append(csv(r.namaMuzakki())).append(",");
            sb.append(csv(r.zakatTypeLabel() == null ? r.zakatType() : r.zakatTypeLabel())).append(",");
            sb.append(r.nominalRp() == null ? "" : r.nominalRp()).append(",");
            sb.append(r.berasKg() == null ? "" : r.berasKg());
            sb.append("\n");
        }

        sb.append("\n");
        sb.append("TOTAL,,,\n");
        sb.append("totalNominalRp,").append(report.totalNominalRp()).append("\n");
        sb.append("totalBerasKg,").append(report.totalBerasKg()).append("\n");
        sb.append("totalJiwa,").append(report.totalJiwa()).append("\n");
        return sb.toString();
    }

    private static String csv(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v);
        boolean mustQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!mustQuote) {
            return s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static List<MuzakkiDetailReportResponse.Row> mapMuzakkiRows(
            List<MuzakkiPersonRepository.MuzakkiReportRow> rows
    ) {
        int[] counter = {0};
        return rows.stream()
                .map(r -> {
                    BigDecimal nominalPerOrang = perOrang(r.getJumlahUang(), r.getJumlahJiwa(), 0);
                    BigDecimal berasPerOrang = perOrang(r.getBeratBerasKg(), r.getJumlahJiwa(), 2);

                    return new MuzakkiDetailReportResponse.Row(
                            ++counter[0],
                            LocalDate.ofInstant(r.getCreatedAt(), DEFAULT_ZONE),
                            r.getNama(),
                            r.getZakatType(),
                            r.getZakatType() == null ? null : r.getZakatType().getLabel(),
                            nominalPerOrang,
                            berasPerOrang
                    );
                })
                .toList();
    }

    private static BigDecimal perOrang(BigDecimal total, Integer jumlahJiwa, int scale) {
        if (total == null) {
            return null;
        }
        if (jumlahJiwa == null || jumlahJiwa <= 0) {
            return total;
        }
        return total.divide(BigDecimal.valueOf(jumlahJiwa), scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // label sekarang di enum ZisType
}
