package com.zakat.controller;

import com.zakat.entity.ZakatQuality;
import com.zakat.enums.ZisType;
import com.zakat.service.dto.ZakatQualityOptionResponse;
import com.zakat.service.dto.ZakatQualityResponse;
import com.zakat.service.dto.ZakatQualityUpsertRequest;
import com.zakat.service.ZakatQualityService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/zakat-qualities")
@RequiredArgsConstructor
public class ZakatQualityController {

    private final com.zakat.repository.ZakatQualityRepository zakatQualityRepository;
    private final ZakatQualityService zakatQualityService;

    @GetMapping
    public List<ZakatQualityOptionResponse> getByType(@RequestParam ZisType zakatType) {
        return zakatQualityRepository.findByZakatTypeAndActiveTrueOrderByNameAsc(zakatType).stream()
                .map(ZakatQualityController::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ZakatQualityResponse getById(@PathVariable UUID id) {
        return toResponseFull(zakatQualityService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ZakatQualityResponse> create(@Valid @RequestBody ZakatQualityUpsertRequest request) {
        ZakatQuality created = zakatQualityService.create(request);
        return ResponseEntity.created(URI.create("/api/zakat-qualities/" + created.getId()))
                .body(toResponseFull(created));
    }

    @PutMapping("/{id}")
    public ZakatQualityResponse update(@PathVariable UUID id, @Valid @RequestBody ZakatQualityUpsertRequest request) {
        return toResponseFull(zakatQualityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        zakatQualityService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    private static ZakatQualityOptionResponse toResponse(ZakatQuality quality) {
        return new ZakatQualityOptionResponse(
                quality.getId(),
                quality.getName(),
                quality.getZakatType(),
                quality.getBeratPerJiwaKg(),
                quality.getNominalPerJiwa()
        );
    }

    private static ZakatQualityResponse toResponseFull(ZakatQuality quality) {
        return new ZakatQualityResponse(
                quality.getId(),
                quality.getName(),
                quality.getZakatType(),
                quality.isActive(),
                quality.getBeratPerJiwaKg(),
                quality.getNominalPerJiwa()
        );
    }
}
