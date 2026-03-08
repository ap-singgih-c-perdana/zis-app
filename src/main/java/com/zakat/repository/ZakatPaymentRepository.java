package com.zakat.repository;


import com.zakat.entity.ZakatPayment;
import com.zakat.enums.ZakatType;
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
                    select distinct p
                    from ZakatPayment p
                    left join p.muzakkiList m
                    where (:fromInclusive is null or p.createdAt >= :fromInclusive)
                      and (:toExclusive is null or p.createdAt < :toExclusive)
                      and (:includeCanceled = true or p.canceled = false)
                      and (
                            :q is null
                            or lower(p.alamat) like lower(concat('%', :q, '%'))
                            or lower(m.nama) like lower(concat('%', :q, '%'))
                          )
                    """,
            countQuery = """
                    select count(distinct p.id)
                    from ZakatPayment p
                    left join p.muzakkiList m
                    where (:fromInclusive is null or p.createdAt >= :fromInclusive)
                      and (:toExclusive is null or p.createdAt < :toExclusive)
                      and (:includeCanceled = true or p.canceled = false)
                      and (
                            :q is null
                            or lower(p.alamat) like lower(concat('%', :q, '%'))
                            or lower(m.nama) like lower(concat('%', :q, '%'))
                          )
                    """
    )
    Page<ZakatPayment> search(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("q") String q,
            @Param("includeCanceled") boolean includeCanceled,
            Pageable pageable
    );

    interface DashboardTotalsRow {
        long getTotalTransaksi();

        BigDecimal getTotalUangMasuk();

        BigDecimal getTotalBerasKg();

        long getTotalJiwaFitrah();
    }

    @Query("""
            select count(p) as totalTransaksi,
                   coalesce(sum(p.jumlahUang), 0) as totalUangMasuk,
                   coalesce(sum(p.beratBerasKg), 0) as totalBerasKg,
                   coalesce(sum(case when p.zakatType in :fitrahTypes then p.jumlahJiwa else 0 end), 0) as totalJiwaFitrah
            from ZakatPayment p
            where p.createdAt >= :fromInclusive
              and p.createdAt < :toExclusive
              and p.canceled = false
            """)
    DashboardTotalsRow dashboardTotals(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("fitrahTypes") List<ZakatType> fitrahTypes
    );

    interface DashboardByTypeRow {
        ZakatType getZakatType();

        long getTransaksi();

        BigDecimal getTotalUang();

        BigDecimal getTotalBerasKg();

        long getTotalJiwa();
    }

    @Query("""
            select p.zakatType as zakatType,
                   count(p) as transaksi,
                   coalesce(sum(p.jumlahUang), 0) as totalUang,
                   coalesce(sum(p.beratBerasKg), 0) as totalBerasKg,
                   coalesce(sum(p.jumlahJiwa), 0) as totalJiwa
            from ZakatPayment p
            where p.createdAt >= :fromInclusive
              and p.createdAt < :toExclusive
              and p.canceled = false
            group by p.zakatType
            """)
    List<DashboardByTypeRow> dashboardByType(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    interface RekapRow {
        ZakatType getZakatType();

        BigDecimal getTotalUang();

        BigDecimal getTotalBerasKg();

        Long getTotalJiwa();
    }

    @Query("""
            select p.zakatType as zakatType,
                   coalesce(sum(p.jumlahUang), 0) as totalUang,
                   coalesce(sum(p.beratBerasKg), 0) as totalBerasKg,
                   coalesce(sum(p.jumlahJiwa), 0) as totalJiwa
            from ZakatPayment p
            where p.createdAt >= :fromInclusive
              and p.createdAt < :toExclusive
              and p.canceled = false
            group by p.zakatType
            """)
    List<RekapRow> rekapByType(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select coalesce(sum(p.jumlahJiwa), 0)
            from ZakatPayment p
            where p.createdAt >= :fromInclusive
              and p.createdAt < :toExclusive
              and p.zakatType in :fitrahTypes
              and p.canceled = false
            """)
    long sumJiwaFitrah(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("fitrahTypes") List<ZakatType> fitrahTypes
    );

    @EntityGraph(attributePaths = {"zakatQuality"})
    Optional<ZakatPayment> findWithQualityById(UUID id);

    @EntityGraph(attributePaths = {"zakatQuality", "muzakkiList"})
    Optional<ZakatPayment> findWithQualityAndMuzakkiById(UUID id);
}
