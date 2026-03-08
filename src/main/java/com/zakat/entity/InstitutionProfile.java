package com.zakat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "institution_profile")
@Getter
@Setter
@NoArgsConstructor
public class InstitutionProfile {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String namaInstansi;

    @Column(nullable = false)
    private String kotaKabupaten;

    @Column(nullable = false)
    private String alamatLengkap;

    private String namaKetua;

    private String namaBendahara;
}
