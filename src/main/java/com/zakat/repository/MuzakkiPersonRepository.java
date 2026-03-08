package com.zakat.repository;

import com.zakat.entity.MuzakkiPerson;
import com.zakat.enums.ZisType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MuzakkiPersonRepository extends JpaRepository<MuzakkiPerson, UUID> {

    interface MuzakkiNameRow {
        UUID getPaymentId();

        String getNama();
    }

    @Query("""
            select m.payment.id as paymentId, m.nama as nama
            from MuzakkiPerson m
            where m.payment.id in :paymentIds
            order by m.id
            """)
    List<MuzakkiNameRow> findNamesByPaymentIds(@Param("paymentIds") List<UUID> paymentIds);

    default List<String> findNamesByPaymentId(UUID paymentId) {
        return findNamesByPaymentIds(List.of(paymentId)).stream().map(MuzakkiNameRow::getNama).toList();
    }

    interface MuzakkiReportRow {
        UUID getPaymentId();

        Instant getCreatedAt();

        String getNama();

        ZisType getZakatType();

        BigDecimal getJumlahUang();

        BigDecimal getBeratBerasKg();

        Integer getJumlahJiwa();
    }

    @Query("""
            select p.id as paymentId,
                   p.createdAt as createdAt,
                   m.nama as nama,
                   p.zakatType as zakatType,
                   p.jumlahUang as jumlahUang,
                   p.beratBerasKg as beratBerasKg,
                   p.jumlahJiwa as jumlahJiwa
            from MuzakkiPerson m
            join m.payment p
            where p.createdAt >= :fromInclusive
              and p.createdAt < :toExclusive
              and p.canceled = false
            order by p.createdAt asc, m.nama asc
            """)
    List<MuzakkiReportRow> findReportRows(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}
