package com.zakat.service;

import com.zakat.entity.InstitutionProfile;
import com.zakat.entity.ZakatPayment;
import com.zakat.enums.PaymentMethod;
import com.zakat.enums.ZisType;
import com.zakat.repository.MuzakkiPersonRepository;
import com.zakat.repository.ZakatPaymentRepository;
import com.zakat.service.dto.InstitutionProfileResponse;
import com.zakat.service.dto.KwitansiReportResponse;
import com.zakat.service.dto.MuzakkiDetailReportResponse;
import com.zakat.service.dto.RekapZisReportResponse;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Jakarta");
    private static final float TEMPLATE_FONT_SIZE = 11f;
    private static final String[] TIMES_NEW_ROMAN_CLASSPATHS = new String[]{
            "static/fonts/Times New Roman.ttf"
    };
    private static final String[] TIMES_NEW_ROMAN_PATHS = new String[]{
            System.getProperty("user.dir", "") + "/src/main/resources/static/fonts/Times New Roman.ttf",
            "/System/Library/Fonts/Supplemental/Times New Roman.ttf",
            "/Library/Fonts/Times New Roman.ttf",
            System.getProperty("user.home", "") + "/Library/Fonts/Times New Roman.ttf"
    };

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

        ZakatPaymentRepository.DashboardTypeBreakdownRow breakdown = zakatPaymentRepository.dashboardTypeBreakdown(fromInclusive, toExclusive);
        BigDecimal zakatFitrahUang = defaultZero(breakdown.getFitrahUang());
        BigDecimal zakatFitrahBerasKg = defaultZero(breakdown.getFitrahBeras());
        BigDecimal fidiah = defaultZero(breakdown.getFidiah());
        BigDecimal zakatMal = defaultZero(breakdown.getZakatMal());
        BigDecimal infaqSedekah = defaultZero(breakdown.getInfaqSedekah());
        BigDecimal totalUangMasuk = zakatFitrahUang.add(fidiah).add(zakatMal).add(infaqSedekah);

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
                fidiah,
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

        ZisType computedType;
        if (payment.getZakatQuality() != null) {
            computedType = payment.getZakatQuality().getZakatType();
        } else {
            computedType = DashboardService.getZisType(payment);
        }

        return new KwitansiReportResponse(
                payment.getId(),
                payment.getReceiptNumber(),
                createdAt,
                tanggal,
                computedType,
                computedType == null ? null : computedType.getLabel(),
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

    @Transactional(readOnly = true)
    public byte[] kwitansiTemplatePdf(UUID paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId wajib diisi");
        }

        ZakatPayment payment = zakatPaymentRepository.findWithQualityById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment tidak ditemukan"));
        KwitansiReportResponse kw = kwitansi(paymentId);

        ClassPathResource template = new ClassPathResource("static/form_zakat_v3.pdf");
        if (!template.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template PDF form_zakat.pdf tidak ditemukan");
        }

        try (InputStream in = template.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfReader reader = new PdfReader(in);
             PdfWriter writer = new PdfWriter(out);
             PdfDocument pdfDoc = new PdfDocument(reader, writer)) {

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            if (form == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template PDF tidak memiliki AcroForm field");
            }
            form.setGenerateAppearance(true);
            Map<String, PdfFormField> fields = form.getFormFields();
            PdfFont textFont = loadTimesNewRomanFont();

            Long perJiwa = null;
            if (payment.getJumlahJiwa() != null && payment.getJumlahJiwa() > 0 && payment.getJumlahUang() != null) {
                perJiwa = payment.getJumlahUang().divide(BigDecimal.valueOf(payment.getJumlahJiwa()), 0, RoundingMode.HALF_UP).longValue();
            } else if (kw.zakatQuality() != null && kw.zakatQuality().nominalPerJiwa() != null) {
                perJiwa = kw.zakatQuality().nominalPerJiwa();
            }

            LocalDate tgl = kw.tanggal();
            if (tgl == null && kw.createdAt() != null) {
                tgl = LocalDate.ofInstant(kw.createdAt(), DEFAULT_ZONE);
            }

            // Exact field mapping from template form_zakat.pdf
            setField(fields, "nama", safe(payment.getPayerName()), textFont);
            setField(fields, "noTelp", safe(payment.getPayerPhone()), textFont);
            setField(fields, "noKwitansi", safe(kw.receiptNumber()), textFont);
            setField(fields, "alamat", safe(payment.getAlamat()), textFont);
            setField(fields, "jumlahBeras", formatNumber(payment.getBeratBerasKg()), textFont);
            setField(fields, "jumlahUang", formatLong(perJiwa), textFont);
            setField(fields, "jumlahJiwaBeras", (payment.getBeratBerasKg() != null && payment.getBeratBerasKg().compareTo(BigDecimal.ZERO) > 0) ? safe(payment.getJumlahJiwa()) : "", textFont);
            setField(fields, "jumlahJiwaUang", (payment.getJumlahUang() != null && payment.getJumlahUang().compareTo(BigDecimal.ZERO) > 0) ? safe(payment.getJumlahJiwa()) : "", textFont);
            setField(fields, "jumlahZakatMal", formatNumber(payment.getJumlahUangZakatMal()), textFont);
            setField(fields, "jumlahInfaqSedekah", formatNumber(payment.getJumlahUangInfaqSedekah()), textFont);
            setField(fields, "jumlahFidiah", formatNumber(payment.getJumlahUangFidiah()), textFont);
            setField(fields, "dd", tgl == null ? "" : String.format("%02d", tgl.getDayOfMonth()), textFont);
            setField(fields, "MM", tgl == null ? "" : String.format("%02d", tgl.getMonthValue()), textFont);
            setField(fields, "yyyy", tgl == null ? "" : String.valueOf(tgl.getYear()), textFont);
            setField(fields, "totalBeras", formatNumber(payment.getBeratBerasKg()), textFont);
            setField(fields, "totalUang", formatNumber(payment.getJumlahUang()), textFont);

            List<String> names = kw.muzakkiNames() == null ? List.of() : kw.muzakkiNames();
            for (int i = 1; i <= 10; i++) {
                String value = i <= names.size() ? names.get(i - 1) : "";
                setField(fields, "muzakki" + i, value, textFont);
            }

            boolean isTransfer = payment.getPaymentMethod() == PaymentMethod.TRANSFER;
            setCheckboxField(fields.get("checkboxCash"), !isTransfer);
            setCheckboxField(fields.get("checkboxTransfer"), isTransfer);

            form.flattenFields();
            pdfDoc.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal mengisi template PDF", e);
        }
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

    private static void setField(Map<String, PdfFormField> fields, String fieldName, String value, PdfFont font) {
        PdfFormField field = fields.get(fieldName);
        if (field == null) return;
        String safeValue = value == null ? "" : value;
        try {
            if (font != null) {
                field.setFontAndSize(font, TEMPLATE_FONT_SIZE);
            }
            field.setValue(safeValue);
        } catch (Exception ignored) {
            field.setValue(safeValue);
        }
    }

    private static void setCheckboxField(PdfFormField field, boolean checked) {
        if (field == null) return;
        String targetValue = "Off";
        if (checked) {
            String[] states = field.getAppearanceStates();
            if (states != null) {
                for (String s : states) {
                    if (!"Off".equalsIgnoreCase(s)) {
                        targetValue = s;
                        break;
                    }
                }
            }
        }
        field.setValue(targetValue);
    }

    private static String safe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String formatNumber(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private static String formatLong(Long v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static PdfFont loadTimesNewRomanFont() {
        for (String classpath : TIMES_NEW_ROMAN_CLASSPATHS) {
            if (classpath == null || classpath.isBlank()) continue;
            ClassPathResource resource = new ClassPathResource(classpath);
            if (!resource.exists()) continue;
            try (InputStream in = resource.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return PdfFontFactory.createFont(
                        bytes,
                        PdfEncodings.WINANSI,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                );
            } catch (Exception ignored) {
            }
        }

        for (String path : TIMES_NEW_ROMAN_PATHS) {
            if (path == null || path.isBlank()) continue;
            java.nio.file.Path p = java.nio.file.Path.of(path);
            if (!java.nio.file.Files.exists(p)) continue;
            try {
                return PdfFontFactory.createFont(
                        path,
                        PdfEncodings.WINANSI,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                );
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // label sekarang di enum ZisType
}
