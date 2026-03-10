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

        Integer getSequenceNo();
    }

    @Query("""
            select m.payment.id as paymentId, m.nama as nama, m.sequenceNo as sequenceNo
            from MuzakkiPerson m
            where m.payment.id in :paymentIds
            order by m.payment.id, coalesce(m.sequenceNo, 2147483647) asc, m.id asc
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
                   (case
                       when p.zakatQuality is not null then p.zakatQuality.zakatType
                       when p.jumlahUangZakatMal is not null and p.jumlahUangZakatMal > 0 then com.zakat.enums.ZisType.ZAKAT_MAL
                       when p.jumlahUangInfaqSedekah is not null and p.jumlahUangInfaqSedekah > 0 then com.zakat.enums.ZisType.INFAQ_SEDEKAH
                       when p.jumlahUangFidiah is not null and p.jumlahUangFidiah > 0 then com.zakat.enums.ZisType.FIDIAH
                       else null end) as zakatType,
                   (coalesce(p.jumlahUang, 0) + coalesce(p.jumlahUangZakatMal, 0) + coalesce(p.jumlahUangInfaqSedekah, 0) + coalesce(p.jumlahUangFidiah, 0)) as jumlahUang,
                   p.beratBerasKg as beratBerasKg,
                   p.jumlahJiwa as jumlahJiwa
            from MuzakkiPerson m
            join m.payment p
            where p.createdAt >= :fromInclusive
              and p.createdAt < :toExclusive
              and p.canceled = false
            order by p.createdAt asc, coalesce(m.sequenceNo, 2147483647) asc, m.id asc
            """)
    List<MuzakkiReportRow> findReportRows(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}
