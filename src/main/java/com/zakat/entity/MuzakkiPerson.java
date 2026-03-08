package com.zakat.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
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

}
