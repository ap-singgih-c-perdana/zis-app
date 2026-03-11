package com.zakat.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
        name = "muzakki_person",
        uniqueConstraints = @UniqueConstraint(name = "uk_muzakki_payment_sequence", columnNames = {"payment_id", "sequence_no"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuzakkiPerson {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(columnDefinition = "text")
    private String nama;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private ZakatPayment payment;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

}
