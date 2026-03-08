package com.zakat.service.dto;

import jakarta.validation.constraints.NotBlank;

public record InstitutionProfileUpsertRequest(
        @NotBlank String namaInstansi,
        @NotBlank String kotaKabupaten,
        @NotBlank String alamatLengkap,
        String namaKetua,
        String namaBendahara
) {
}

