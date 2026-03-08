package com.zakat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "receipt_sequence")
@Getter
@Setter
@NoArgsConstructor
public class ReceiptSequence {

    @Id
    @Column(name = "receipt_year")
    private Integer receiptYear;

    @Version
    private Long version;

    @Column(name = "last_issued", nullable = false)
    private Long lastIssued;

    public ReceiptSequence(Integer receiptYear, Long lastIssued) {
        this.receiptYear = receiptYear;
        this.lastIssued = lastIssued;
    }
}
