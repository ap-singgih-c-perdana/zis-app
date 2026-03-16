package com.zakat.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InstitutionProfileUpsertRequest(
        @NotBlank String namaInstansi,
        @NotBlank String kotaKabupaten,
        @NotBlank String alamatLengkap,
        String nomorTelepon,
        @Email String email,
        String namaKetua,
        String namaBendahara
) {
}
