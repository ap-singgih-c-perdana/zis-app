package com.zakat.controller;

import com.zakat.entity.MuzakkiPerson;
import com.zakat.entity.ZakatPayment;
import com.zakat.entity.ZakatQuality;
import com.zakat.service.ZakatPaymentService;
import com.zakat.service.dto.CancelZakatPaymentRequest;
import com.zakat.service.dto.CreateZakatPaymentRequest;
import com.zakat.service.dto.UpdateZakatPaymentRequest;
import com.zakat.service.dto.ZakatPaymentListItemResponse;
import com.zakat.service.dto.ZakatPaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.time.LocalDate;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/zakat-payments")
@RequiredArgsConstructor
public class ZakatPaymentController {

    private final ZakatPaymentService zakatPaymentService;

    @PostMapping
    public ResponseEntity<ZakatPaymentResponse> create(@Valid @RequestBody CreateZakatPaymentRequest request) {
        ZakatPayment payment = zakatPaymentService.create(request);
        return ResponseEntity
                .created(URI.create("/api/zakat-payments/" + payment.getId()))
                .body(toResponse(payment));
    }

    @PutMapping("/{id}")
    public ZakatPaymentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateZakatPaymentRequest request) {
        return toResponse(zakatPaymentService.update(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelZakatPaymentRequest request,
            Principal principal
    ) {
        String canceledBy = principal == null ? null : principal.getName();
        zakatPaymentService.cancel(id, request.reason(), canceledBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ZakatPaymentResponse getById(@PathVariable UUID id) {
        return toResponse(zakatPaymentService.getById(id));
    }

    @GetMapping
    public Page<ZakatPaymentListItemResponse> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean includeCanceled,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return zakatPaymentService.search(from, to, q, includeCanceled, pageable);
    }

    private static ZakatPaymentResponse toResponse(ZakatPayment payment) {
        ZakatQuality quality = payment.getZakatQuality();
        ZakatPaymentResponse.ZakatQualitySummary qualitySummary = null;
        if (quality != null) {
            qualitySummary = new ZakatPaymentResponse.ZakatQualitySummary(
                    quality.getId(),
                    quality.getName(),
                    quality.getBeratPerJiwaKg()
            );
        }

        List<String> muzakkiNames = payment.getMuzakkiList() == null
                ? List.of()
                : payment.getMuzakkiList().stream().map(MuzakkiPerson::getNama).toList();

        return new ZakatPaymentResponse(
                payment.getId(),
                payment.getReceiptNumber(),
                payment.isCanceled(),
                payment.getCreatedAt(),
                payment.getZakatType(),
                payment.getJumlahJiwa(),
                payment.getAlamat(),
                payment.getBeratBerasKg(),
                payment.getJumlahUang(),
                qualitySummary,
                muzakkiNames
        );
    }

}
