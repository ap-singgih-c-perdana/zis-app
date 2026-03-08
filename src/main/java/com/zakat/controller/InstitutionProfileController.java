package com.zakat.controller;

import com.zakat.entity.InstitutionProfile;
import com.zakat.service.InstitutionProfileService;
import com.zakat.service.dto.InstitutionProfileResponse;
import com.zakat.service.dto.InstitutionProfileUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institution-profile")
@RequiredArgsConstructor
public class InstitutionProfileController {

    private final InstitutionProfileService institutionProfileService;

    @GetMapping
    public ResponseEntity<InstitutionProfileResponse> get() {
        InstitutionProfile profile = institutionProfileService.get();
        if (profile == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toResponse(profile));
    }

    @PutMapping
    public InstitutionProfileResponse upsert(@Valid @RequestBody InstitutionProfileUpsertRequest request) {
        return toResponse(institutionProfileService.upsert(request));
    }

    private static InstitutionProfileResponse toResponse(InstitutionProfile profile) {
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

