package com.zakat.repository;

import com.zakat.enums.ZakatType;
import com.zakat.entity.ZakatQuality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ZakatQualityRepository extends JpaRepository<ZakatQuality, UUID> {
    List<ZakatQuality> findByZakatTypeAndActiveTrueOrderByNameAsc(ZakatType zakatType);
}
