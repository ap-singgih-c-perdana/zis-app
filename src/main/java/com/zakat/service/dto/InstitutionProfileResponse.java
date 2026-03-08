package com.zakat.service.dto;

import java.util.UUID;

public record InstitutionProfileResponse(
        UUID id,
        String namaInstansi,
        String kotaKabupaten,
        String alamatLengkap,
        String namaKetua,
        String namaBendahara
) {
}

