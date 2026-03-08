package com.zakat.service;

import com.zakat.entity.ZakatQuality;
import com.zakat.enums.ZakatType;
import com.zakat.repository.ZakatQualityRepository;
import com.zakat.service.dto.ZakatQualityUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class ZakatQualityService {

    private final ZakatQualityRepository zakatQualityRepository;

    @Transactional(readOnly = true)
    public ZakatQuality getById(UUID id) {
        return zakatQualityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zakat quality tidak ditemukan"));
    }

    @Transactional
    public ZakatQuality create(@Valid ZakatQualityUpsertRequest request) {
        validateByType(request);

        ZakatQuality quality = new ZakatQuality();
        quality.setName(request.name());
        quality.setZakatType(request.zakatType());
        quality.setBeratPerJiwaKg(request.beratPerJiwaKg());
        quality.setNominalPerJiwa(request.nominalPerJiwa());
        quality.setActive(request.active() == null || request.active());
        return zakatQualityRepository.save(quality);
    }

    @Transactional
    public ZakatQuality update(UUID id, @Valid ZakatQualityUpsertRequest request) {
        validateByType(request);

        ZakatQuality quality = getById(id);
        quality.setName(request.name());
        quality.setZakatType(request.zakatType());
        quality.setBeratPerJiwaKg(request.beratPerJiwaKg());
        quality.setNominalPerJiwa(request.nominalPerJiwa());
        if (request.active() != null) {
            quality.setActive(request.active());
        }
        return zakatQualityRepository.save(quality);
    }

    @Transactional
    public void deactivate(UUID id) {
        ZakatQuality quality = getById(id);
        quality.setActive(false);
        zakatQualityRepository.save(quality);
    }

    private static void validateByType(ZakatQualityUpsertRequest request) {
        ZakatType type = request.zakatType();
        BigDecimal berat = request.beratPerJiwaKg();
        Long nominal = request.nominalPerJiwa();

        if (type == ZakatType.ZAKAT_FITRAH_BERAS) {
            if (berat == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beratPerJiwaKg wajib diisi untuk ZAKAT_FITRAH_BERAS");
            }
            if (nominal != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nominalPerJiwa harus null untuk ZAKAT_FITRAH_BERAS");
            }
        } else if (type == ZakatType.ZAKAT_FITRAH_UANG) {
            if (nominal == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nominalPerJiwa wajib diisi untuk ZAKAT_FITRAH_UANG");
            }
            if (berat != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beratPerJiwaKg harus null untuk ZAKAT_FITRAH_UANG");
            }
        }
    }
}

