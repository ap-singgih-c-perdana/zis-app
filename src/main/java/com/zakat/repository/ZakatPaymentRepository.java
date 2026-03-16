package com.zakat.repository;


import com.zakat.entity.ZakatPayment;
import com.zakat.enums.ZisType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZakatPaymentRepository extends JpaRepository<ZakatPayment, UUID> {

    @Query(
            value = """
                    select p
                    from ZakatPayment p
                    where p.paymentAt >= :fromInclusive
                      and p.paymentAt < :toExclusive
                      and (:includeCanceled = true or p.canceled = false)
                      and (
                            lower(p.alamat) like :qLike
                            or lower(p.payerName) like :qLike
                            or lower(coalesce(p.payerPhone, '')) like :qLike
                            or exists (
                                select 1
                                from MuzakkiPerson m
                                where m.payment = p
                                  and lower(m.nama) like :qLike
                            )
                          )
                    and (:payerLike is null or lower(p.payerName) like :payerLike)
                    and (:phoneLike is null or lower(coalesce(p.payerPhone, '')) like :phoneLike)
                    order by
                      case
                        when :sortKey = 'receiptNumber' and p.receiptNumber is null then 1
                        when :sortKey = 'paymentAt' and p.paymentAt is null then 1
                        when :sortKey = 'jumlahUang' and p.jumlahUang is null then 1
                        when :sortKey = 'beratBerasKg' and p.beratBerasKg is null then 1
                        when :sortKey = 'jumlahUangZakatMal' and p.jumlahUangZakatMal is null then 1
                        when :sortKey = 'jumlahUangInfaqSedekah' and p.jumlahUangInfaqSedekah is null then 1
                        when :sortKey = 'jumlahUangFidiah' and p.jumlahUangFidiah is null then 1
                        else 0
                      end asc,
                      case when :sortKey = 'receiptNumber' and :sortDir = 'asc' then p.receiptNumber end asc,
                      case when :sortKey = 'receiptNumber' and :sortDir = 'desc' then p.receiptNumber end desc,
                      case when :sortKey = 'paymentAt' and :sortDir = 'asc' then p.paymentAt end asc,
                      case when :sortKey = 'paymentAt' and :sortDir = 'desc' then p.paymentAt end desc,
                      case when :sortKey = 'jumlahUang' and :sortDir = 'asc' then p.jumlahUang end asc,
                      case when :sortKey = 'jumlahUang' and :sortDir = 'desc' then p.jumlahUang end desc,
                      case when :sortKey = 'beratBerasKg' and :sortDir = 'asc' then p.beratBerasKg end asc,
                      case when :sortKey = 'beratBerasKg' and :sortDir = 'desc' then p.beratBerasKg end desc,
                      case when :sortKey = 'jumlahUangZakatMal' and :sortDir = 'asc' then p.jumlahUangZakatMal end asc,
                      case when :sortKey = 'jumlahUangZakatMal' and :sortDir = 'desc' then p.jumlahUangZakatMal end desc,
                      case when :sortKey = 'jumlahUangInfaqSedekah' and :sortDir = 'asc' then p.jumlahUangInfaqSedekah end asc,
                      case when :sortKey = 'jumlahUangInfaqSedekah' and :sortDir = 'desc' then p.jumlahUangInfaqSedekah end desc,
                      case when :sortKey = 'jumlahUangFidiah' and :sortDir = 'asc' then p.jumlahUangFidiah end asc,
                      case when :sortKey = 'jumlahUangFidiah' and :sortDir = 'desc' then p.jumlahUangFidiah end desc,
                      case when :sortKey = 'paymentAt' and :sortDir = 'asc' then p.receiptYear end asc,
                      case when :sortKey = 'paymentAt' and :sortDir = 'asc' then p.receiptSequence end asc,
                      case when :sortKey = 'paymentAt' and :sortDir = 'desc' then p.receiptYear end desc,
                      case when :sortKey = 'paymentAt' and :sortDir = 'desc' then p.receiptSequence end desc,
                      p.paymentAt desc, p.receiptYear desc, p.receiptSequence desc, p.id desc
                    """,
            countQuery = """
                    select count(p.id)
                    from ZakatPayment p
                    where p.paymentAt >= :fromInclusive
                      and p.paymentAt < :toExclusive
                      and (:includeCanceled = true or p.canceled = false)
                      and (
                            lower(p.alamat) like :qLike
                            or lower(p.payerName) like :qLike
                            or lower(coalesce(p.payerPhone, '')) like :qLike
                            or exists (
                                select 1
                                from MuzakkiPerson m
                                where m.payment = p
                                  and lower(m.nama) like :qLike
                            )
                          )
                      and (:payerLike is null or lower(p.payerName) like :payerLike)
                      and (:phoneLike is null or lower(coalesce(p.payerPhone, '')) like :phoneLike)
                   """
    )
    Page<ZakatPayment> search(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("qLike") String qLike,
            @Param("payerLike") String payerLike,
            @Param("phoneLike") String phoneLike,
            @Param("includeCanceled") boolean includeCanceled,
            @Param("sortKey") String sortKey,
            @Param("sortDir") String sortDir,
            Pageable pageable
    );

    @Query("""
            select coalesce(max(p.receiptSequence), 0)
            from ZakatPayment p
            where p.receiptYear = :year
            """)
    long maxReceiptSequenceForYear(@Param("year") Integer year);

    @Query("""
            select min(p.paymentAt)
            from ZakatPayment p
            where p.canceled = false
            """)
    Instant minPaymentAt();

    interface DashboardTotalsRow {
        long getTotalTransaksi();

        BigDecimal getTotalUangMasuk();

        BigDecimal getTotalBerasKg();

        long getTotalJiwaFitrah();
    }

    @Query("""
            select count(p) as totalTransaksi,
                   (coalesce(sum(p.jumlahUang), 0) + coalesce(sum(p.jumlahUangZakatMal), 0) + coalesce(sum(p.jumlahUangInfaqSedekah), 0) + coalesce(sum(p.jumlahUangFidiah), 0)) as totalUangMasuk,
                   coalesce(sum(p.beratBerasKg), 0) as totalBerasKg,
                   coalesce(sum(case when (q.zakatType in :fitrahTypes) then p.jumlahJiwa else 0 end), 0) as totalJiwaFitrah
            from ZakatPayment p
            left join p.zakatQuality q
            where p.paymentAt >= :fromInclusive
              and p.paymentAt < :toExclusive
              and p.canceled = false
            """)
    DashboardTotalsRow dashboardTotals(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("fitrahTypes") List<ZisType> fitrahTypes
    );

    interface DashboardByTypeRow {
        ZisType getZakatType();

        BigDecimal getFitrahUang();

        BigDecimal getMalUang();

        BigDecimal getInfaqUang();

        BigDecimal getFidiahUang();

        BigDecimal getTotalBerasKg();
    }

    @Query("""
            select (case
                       when p.zakatQuality is not null then p.zakatQuality.zakatType
                       when p.jumlahUangZakatMal is not null and p.jumlahUangZakatMal > 0 then com.zakat.enums.ZisType.ZAKAT_MAL
                       when p.jumlahUangInfaqSedekah is not null and p.jumlahUangInfaqSedekah > 0 then com.zakat.enums.ZisType.INFAQ_SEDEKAH
                       when p.jumlahUangFidiah is not null and p.jumlahUangFidiah > 0 then com.zakat.enums.ZisType.FIDIAH
                       else null end) as zakatType,
                   coalesce(sum(case when (p.zakatQuality is not null and p.zakatQuality.zakatType = com.zakat.enums.ZisType.ZAKAT_FITRAH_UANG) then p.jumlahUang else 0 end), 0) as fitrahUang,
                   coalesce(sum(case when (p.jumlahUangZakatMal is not null and p.jumlahUangZakatMal > 0) then p.jumlahUangZakatMal else 0 end), 0) as malUang,
                   coalesce(sum(case when (p.jumlahUangInfaqSedekah is not null and p.jumlahUangInfaqSedekah > 0) then p.jumlahUangInfaqSedekah else 0 end), 0) as infaqUang,
                   coalesce(sum(case when (p.jumlahUangFidiah is not null and p.jumlahUangFidiah > 0) then p.jumlahUangFidiah else 0 end), 0) as fidiahUang,
                   coalesce(sum(p.beratBerasKg), 0) as totalBerasKg
            from ZakatPayment p
            where p.paymentAt >= :fromInclusive
              and p.paymentAt < :toExclusive
              and p.canceled = false
            group by (case
                        when p.zakatQuality is not null then p.zakatQuality.zakatType
                        when p.jumlahUangZakatMal is not null and p.jumlahUangZakatMal > 0 then com.zakat.enums.ZisType.ZAKAT_MAL
                        when p.jumlahUangInfaqSedekah is not null and p.jumlahUangInfaqSedekah > 0 then com.zakat.enums.ZisType.INFAQ_SEDEKAH
                        when p.jumlahUangFidiah is not null and p.jumlahUangFidiah > 0 then com.zakat.enums.ZisType.FIDIAH
                        else null end)
             """)
     List<DashboardByTypeRow> dashboardByType(
             @Param("fromInclusive") Instant fromInclusive,
             @Param("toExclusive") Instant toExclusive
     );

    interface DashboardTypeBreakdownRow {
        BigDecimal getFitrahUang();

        BigDecimal getFitrahBeras();

        BigDecimal getFidiah();

        BigDecimal getZakatMal();

        BigDecimal getInfaqSedekah();
    }

    interface DashboardPaymentMethodBreakdownRow {
        BigDecimal getTotalUangCash();

        BigDecimal getTotalUangTransfer();
    }

    @Query("""
            select coalesce(sum(p.jumlahUang), 0) as fitrahUang,
                   coalesce(sum(p.beratBerasKg), 0) as fitrahBeras,
                   coalesce(sum(p.jumlahUangFidiah), 0) as fidiah,
                   coalesce(sum(p.jumlahUangZakatMal), 0) as zakatMal,
                   coalesce(sum(p.jumlahUangInfaqSedekah), 0) as infaqSedekah
            from ZakatPayment p
            where p.paymentAt >= :fromInclusive
              and p.paymentAt < :toExclusive
              and p.canceled = false
            """)
    DashboardTypeBreakdownRow dashboardTypeBreakdown(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select coalesce(sum(case
                                  when p.paymentMethod = com.zakat.enums.PaymentMethod.CASH
                                  then (coalesce(p.jumlahUang, 0) + coalesce(p.jumlahUangZakatMal, 0) + coalesce(p.jumlahUangInfaqSedekah, 0) + coalesce(p.jumlahUangFidiah, 0))
                                  else 0
                               end), 0) as totalUangCash,
                   coalesce(sum(case
                                  when p.paymentMethod = com.zakat.enums.PaymentMethod.TRANSFER
                                  then (coalesce(p.jumlahUang, 0) + coalesce(p.jumlahUangZakatMal, 0) + coalesce(p.jumlahUangInfaqSedekah, 0) + coalesce(p.jumlahUangFidiah, 0))
                                  else 0
                               end), 0) as totalUangTransfer
            from ZakatPayment p
            where p.paymentAt >= :fromInclusive
              and p.paymentAt < :toExclusive
              and p.canceled = false
            """)
    DashboardPaymentMethodBreakdownRow dashboardPaymentMethodBreakdown(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    interface RekapRow {
        ZisType getZakatType();

        BigDecimal getTotalUang();

        BigDecimal getTotalBerasKg();

        Long getTotalJiwa();
    }

    @Query("""
            select (case
                       when p.zakatQuality is not null then p.zakatQuality.zakatType
                       when p.jumlahUangZakatMal is not null and p.jumlahUangZakatMal > 0 then com.zakat.enums.ZisType.ZAKAT_MAL
                       when p.jumlahUangInfaqSedekah is not null and p.jumlahUangInfaqSedekah > 0 then com.zakat.enums.ZisType.INFAQ_SEDEKAH
                       when p.jumlahUangFidiah is not null and p.jumlahUangFidiah > 0 then com.zakat.enums.ZisType.FIDIAH
                       else null end) as zakatType,
                   (coalesce(sum(p.jumlahUang), 0) + coalesce(sum(p.jumlahUangZakatMal), 0) + coalesce(sum(p.jumlahUangInfaqSedekah), 0) + coalesce(sum(p.jumlahUangFidiah), 0)) as totalUang,
                   coalesce(sum(p.beratBerasKg), 0) as totalBerasKg,
                   coalesce(sum(p.jumlahJiwa), 0) as totalJiwa
            from ZakatPayment p
            where p.paymentAt >= :fromInclusive
              and p.paymentAt < :toExclusive
              and p.canceled = false
            group by (case
                        when p.zakatQuality is not null then p.zakatQuality.zakatType
                        when p.jumlahUangZakatMal is not null and p.jumlahUangZakatMal > 0 then com.zakat.enums.ZisType.ZAKAT_MAL
                        when p.jumlahUangInfaqSedekah is not null and p.jumlahUangInfaqSedekah > 0 then com.zakat.enums.ZisType.INFAQ_SEDEKAH
                        when p.jumlahUangFidiah is not null and p.jumlahUangFidiah > 0 then com.zakat.enums.ZisType.FIDIAH
                        else null end)
            """)
    List<RekapRow> rekapByType(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select coalesce(sum(p.jumlahJiwa), 0)
            from ZakatPayment p
            where p.paymentAt >= :fromInclusive
              and p.paymentAt < :toExclusive
              and p.zakatQuality is not null
              and p.zakatQuality.zakatType in :fitrahTypes
              and p.canceled = false
            """)
    long sumJiwaFitrah(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("fitrahTypes") List<ZisType> fitrahTypes
    );

    @EntityGraph(attributePaths = {"zakatQuality"})
    Optional<ZakatPayment> findWithQualityById(UUID id);

    @EntityGraph(attributePaths = {"zakatQuality", "muzakkiList"})
    Optional<ZakatPayment> findWithQualityAndMuzakkiById(UUID id);

    @EntityGraph(attributePaths = {"muzakkiList"})
    @Query("""
            select p
            from ZakatPayment p
            where p.paymentAt >= :fromInclusive
              and p.paymentAt < :toExclusive
              and p.canceled = false
            order by p.paymentAt desc
            """)
    List<ZakatPayment> findRecent(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            Pageable pageable
    );
}
