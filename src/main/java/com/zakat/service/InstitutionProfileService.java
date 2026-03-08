package com.zakat.service;

import com.zakat.entity.InstitutionProfile;
import com.zakat.repository.InstitutionProfileRepository;
import com.zakat.service.dto.InstitutionProfileUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class InstitutionProfileService {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final InstitutionProfileRepository institutionProfileRepository;

    @Transactional(readOnly = true)
    public InstitutionProfile get() {
        return institutionProfileRepository.findById(SINGLETON_ID).orElse(null);
    }

    @Transactional
    public InstitutionProfile upsert(@Valid InstitutionProfileUpsertRequest request) {
        InstitutionProfile profile = institutionProfileRepository.findById(SINGLETON_ID).orElseGet(() -> {
            InstitutionProfile created = new InstitutionProfile();
            created.setId(SINGLETON_ID);
            return created;
        });

        profile.setNamaInstansi(request.namaInstansi());
        profile.setKotaKabupaten(request.kotaKabupaten());
        profile.setAlamatLengkap(request.alamatLengkap());
        profile.setNamaKetua(request.namaKetua());
        profile.setNamaBendahara(request.namaBendahara());

        return institutionProfileRepository.save(profile);
    }
}

