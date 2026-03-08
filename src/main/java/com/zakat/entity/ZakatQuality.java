package com.zakat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import com.zakat.enums.ZakatType;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZakatQuality {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ZakatType zakatType;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Untuk opsi zakat fitrah (beras): berat per jiwa dalam Kg.
     * Untuk opsi zakat fitrah (uang): null.
     */
    private BigDecimal beratPerJiwaKg;

    /**
     * Untuk opsi zakat fitrah (uang): nominal per jiwa dalam Rupiah (tanpa desimal).
     * Untuk opsi zakat fitrah (beras): null.
     */
    private Long nominalPerJiwa;

}
