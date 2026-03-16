package com.zakat.service.dto;

import java.util.UUID;

public record InstitutionProfileResponse(
        UUID id,
        String namaInstansi,
        String kotaKabupaten,
        String alamatLengkap,
        String nomorTelepon,
        String email,
        String namaKetua,
        String namaBendahara
) {
}
