package com.zakat.service;

import com.zakat.entity.MuzakkiPerson;
import com.zakat.entity.ZakatPayment;
import com.zakat.entity.ZakatQuality;
import com.zakat.entity.ReceiptSequence;
import com.zakat.enums.ZisType;
import com.zakat.repository.MuzakkiPersonRepository;
import com.zakat.repository.ZakatPaymentRepository;
import com.zakat.repository.ZakatQualityRepository;
import com.zakat.repository.ReceiptSequenceRepository;
import com.zakat.service.dto.CreateZakatPaymentRequest;
import com.zakat.service.dto.ZakatPaymentListItemResponse;
import com.zakat.service.dto.UpdateZakatPaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class ZakatPaymentService {

    private static final int PREVIEW_LIMIT = 3;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Jakarta");
    private static final Instant MAX_INSTANT = Instant.parse("9999-12-31T23:59:59.999999999Z");

    private final ZakatPaymentRepository zakatPaymentRepository;
    private final ZakatQualityRepository zakatQualityRepository;
    private final MuzakkiPersonRepository muzakkiPersonRepository;
    private final ReceiptSequenceRepository receiptSequenceRepository;

    @Transactional(readOnly = true)
    public Page<ZakatPaymentListItemResponse> search(
            LocalDate fromDate,
            LocalDate toDate,
            String q,
            String payerName,
            String payerPhone,
            boolean includeCanceled,
            Pageable pageable
    ) {
        Instant fromInclusive = (fromDate == null)
                ? Instant.EPOCH
                : fromDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant toExclusive = (toDate == null)
                ? MAX_INSTANT
                : toDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
        String qLike = normalizedQ == null ? "%" : "%" + normalizedQ + "%";
        String payerLike = (payerName == null || payerName.isBlank()) ? null : ("%" + payerName.trim().toLowerCase() + "%");
        String phoneLike = (payerPhone == null || payerPhone.isBlank()) ? null : ("%" + payerPhone.trim().toLowerCase() + "%");

        Page<ZakatPayment> page = zakatPaymentRepository.search(fromInclusive, toExclusive, qLike, payerLike, phoneLike, includeCanceled, pageable);
        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> paymentIds = page.getContent().stream().map(ZakatPayment::getId).toList();
        Map<UUID, List<String>> namesByPaymentId = muzakkiPersonRepository.findNamesByPaymentIds(paymentIds).stream()
                .collect(Collectors.groupingBy(
                        MuzakkiPersonRepository.MuzakkiNameRow::getPaymentId,
                        Collectors.mapping(MuzakkiPersonRepository.MuzakkiNameRow::getNama, Collectors.toList())
                ));


        List<ZakatPaymentListItemResponse> content = page.getContent().stream()
                .map(payment -> {
                    List<String> names = namesByPaymentId.getOrDefault(payment.getId(), List.of());
                    names = names.stream()
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .toList();
                    int count = names.size();
                    String preview = String.join(", ", names.stream().limit(PREVIEW_LIMIT).toList());
                    if (count > PREVIEW_LIMIT) {
                        preview = preview + ", +" + (count - PREVIEW_LIMIT);
                    }
                    return new ZakatPaymentListItemResponse(
                            payment.getId(),
                            payment.getReceiptNumber(),
                            payment.getCreatedAt(),
                            payment.getZakatType(),
                            payment.getZakatType() == null ? null : payment.getZakatType().getLabel(),
                            payment.getBeratBerasKg(),
                            payment.getJumlahUang(),
                            count,
                            preview,
                            payment.getAlamat(),
                            payment.getPayerName(),
                            payment.getPayerPhone(),
                            payment.isCanceled()
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional
    public ZakatPayment create(@Valid CreateZakatPaymentRequest request) {
        if (request.muzakkiNames() == null || request.muzakkiNames().size() != request.jumlahJiwa()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jumlah muzakki harus sama dengan jumlahJiwa");
        }

        ZisType zakatType = request.zakatType();

        ZakatQuality zakatQuality = null;
        if (request.zakatQualityId() != null) {
            zakatQuality = zakatQualityRepository.findById(request.zakatQualityId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zakat quality tidak ditemukan"));
            if (!zakatQuality.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zakat quality sudah tidak aktif");
            }
            if (zakatQuality.getZakatType() != zakatType) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zakat quality tidak sesuai zakatType");
            }
        }

        ZakatPayment payment = new ZakatPayment();
        assignReceiptNumber(payment);
        // payer info
        payment.setPayerName(request.payerName());
        payment.setPayerPhone(request.payerPhone());
        payment.setJumlahJiwa(request.jumlahJiwa());
        payment.setAlamat(request.alamat());
        payment.setZakatType(zakatType);
        payment.setZakatQuality(zakatQuality);

        if (zakatType == ZisType.ZAKAT_FITRAH_BERAS) {
            BigDecimal beratBerasKg = request.beratBerasKg();
            if (beratBerasKg == null && zakatQuality != null && zakatQuality.getBeratPerJiwaKg() != null) {
                beratBerasKg = zakatQuality.getBeratPerJiwaKg().multiply(BigDecimal.valueOf(request.jumlahJiwa()));
            }
            if (beratBerasKg == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beratBerasKg atau zakatQualityId wajib diisi");
            }
            payment.setBeratBerasKg(beratBerasKg);
            payment.setJumlahUang(null);
        } else if (zakatType == ZisType.ZAKAT_FITRAH_UANG) {
            BigDecimal jumlahUang = request.jumlahUang();
            if (jumlahUang == null && zakatQuality != null && zakatQuality.getNominalPerJiwa() != null) {
                jumlahUang = BigDecimal.valueOf(zakatQuality.getNominalPerJiwa())
                        .multiply(BigDecimal.valueOf(request.jumlahJiwa()));
            }
            if (jumlahUang == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jumlahUang atau zakatQualityId wajib diisi");
            }
            payment.setJumlahUang(jumlahUang);
            payment.setBeratBerasKg(null);
        } else {
            if (request.jumlahUang() == null && request.beratBerasKg() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beratBerasKg atau jumlahUang wajib diisi");
            }
            payment.setJumlahUang(request.jumlahUang());
            payment.setBeratBerasKg(request.beratBerasKg());
        }

        List<MuzakkiPerson> muzakkiList = request.muzakkiNames().stream()
                .map(nama -> MuzakkiPerson.builder().nama(nama).payment(payment).build())
                .collect(Collectors.toCollection(ArrayList::new));
        payment.setMuzakkiList(muzakkiList);

        return zakatPaymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public ZakatPayment getById(UUID paymentId) {
        if (paymentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentId wajib diisi");
        }
        return zakatPaymentRepository.findWithQualityAndMuzakkiById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment tidak ditemukan"));
    }

    @Transactional
    public ZakatPayment update(UUID paymentId, @Valid UpdateZakatPaymentRequest request) {
        if (paymentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentId wajib diisi");
        }

        ZakatPayment payment = getById(paymentId);

        if (payment.isCanceled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment sudah dibatalkan");
        }

        int jumlahJiwa = request.muzakkiNames().size();
        payment.setJumlahJiwa(jumlahJiwa);
        payment.setAlamat(request.alamat());

        List<MuzakkiPerson> muzakkiList = request.muzakkiNames().stream()
                .map(nama -> MuzakkiPerson.builder().nama(nama).payment(payment).build())
                .collect(Collectors.toCollection(ArrayList::new));
        if (payment.getMuzakkiList() == null) {
            payment.setMuzakkiList(muzakkiList);
        } else {
            payment.getMuzakkiList().clear();
            payment.getMuzakkiList().addAll(muzakkiList);
        }

        ZisType zakatType = payment.getZakatType();
        if (zakatType == ZisType.ZAKAT_FITRAH_BERAS || zakatType == ZisType.ZAKAT_FITRAH_UANG) {
            if (request.zakatQualityId() != null) {
                ZakatQuality quality = zakatQualityRepository.findById(request.zakatQualityId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zakat quality tidak ditemukan"));
                if (!quality.isActive()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zakat quality sudah tidak aktif");
                }
                if (quality.getZakatType() != zakatType) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zakat quality tidak sesuai zakatType");
                }
                payment.setZakatQuality(quality);
            }

            ZakatQuality selectedQuality = payment.getZakatQuality();
            if (selectedQuality == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zakatQualityId wajib diisi untuk zakat fitrah");
            }

            if (zakatType == ZisType.ZAKAT_FITRAH_BERAS) {
                if (selectedQuality.getBeratPerJiwaKg() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zakat quality tidak memiliki beratPerJiwaKg");
                }
                payment.setBeratBerasKg(selectedQuality.getBeratPerJiwaKg().multiply(BigDecimal.valueOf(jumlahJiwa)));
                payment.setJumlahUang(null);
            } else {
                if (selectedQuality.getNominalPerJiwa() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zakat quality tidak memiliki nominalPerJiwa");
                }
                payment.setJumlahUang(BigDecimal.valueOf(selectedQuality.getNominalPerJiwa()).multiply(BigDecimal.valueOf(jumlahJiwa)));
                payment.setBeratBerasKg(null);
            }
        } else {
            payment.setZakatQuality(null);
            if (request.jumlahUang() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jumlahUang wajib diisi untuk jenis zakat ini");
            }
            payment.setJumlahUang(request.jumlahUang());
            payment.setBeratBerasKg(null);
        }

        // update payer info when editing
        payment.setPayerName(request.payerName());
        payment.setPayerPhone(request.payerPhone());

        return zakatPaymentRepository.save(payment);
    }

    @Transactional
    public void cancel(UUID paymentId, String reason, String canceledBy) {
        if (paymentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentId wajib diisi");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason wajib diisi");
        }

        ZakatPayment payment = zakatPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment tidak ditemukan"));

        if (payment.isCanceled()) {
            return;
        }

        payment.setCanceled(true);
        payment.setCanceledAt(Instant.now());
        payment.setCancelReason(reason.trim());
        payment.setCanceledBy(canceledBy);
        zakatPaymentRepository.save(payment);
    }

    private void assignReceiptNumber(ZakatPayment payment) {
        int year = LocalDate.now(DEFAULT_ZONE).getYear();

        ReceiptSequence sequence = receiptSequenceRepository.findForUpdate(year)
                .orElseGet(() -> {
                    long maxExisting = zakatPaymentRepository.maxReceiptSequenceForYear(year);
                    try {
                        return receiptSequenceRepository.saveAndFlush(new ReceiptSequence(year, maxExisting));
                    } catch (DataIntegrityViolationException e) {
                        // Race condition: sequence already created by another transaction
                        return receiptSequenceRepository.findForUpdate(year)
                                .orElseThrow(() -> e);
                    }
                });

        long maxExisting = zakatPaymentRepository.maxReceiptSequenceForYear(year);
        long base = Math.max(sequence.getLastIssued(), maxExisting);
        sequence.setLastIssued(base);

        long next = base + 1;
        sequence.setLastIssued(next);
        receiptSequenceRepository.save(sequence);

        payment.setReceiptYear(year);
        payment.setReceiptSequence(next);
        payment.setReceiptNumber(String.format("KW/%d/%06d", year, next));
    }
}
