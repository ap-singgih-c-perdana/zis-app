package com.zakat.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import com.zakat.enums.ZisType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "zakat_payment")
@Getter
@Setter
@NoArgsConstructor
public class ZakatPayment {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private Integer jumlahJiwa;

    @Column(columnDefinition = "text")
    private String alamat;

    private String payerName;

    private String payerPhone;

    private BigDecimal beratBerasKg;

    private BigDecimal jumlahUang;

    @Enumerated(EnumType.STRING)
    private ZisType zakatType;

    @CreationTimestamp
    private Instant createdAt;

    /**
     * Jika payment dibatalkan (void), maka tidak ikut dihitung di report.
     */
    @Column(nullable = false)
    private boolean canceled = false;

    private Instant canceledAt;

    @Column(columnDefinition = "text")
    private String cancelReason;

    private String canceledBy;

    /**
     * Nomor kwitansi format: KW/{year}/{sequence(6 digits)}.
     */
    @Column(unique = true)
    private String receiptNumber;

    private Integer receiptYear;

    private Long receiptSequence;

    @ManyToOne
    @JoinColumn(name = "zakat_quality_id")
    private ZakatQuality zakatQuality;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MuzakkiPerson> muzakkiList;

}
