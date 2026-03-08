package com.zakat.repository;

import com.zakat.entity.ReceiptSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReceiptSequenceRepository extends JpaRepository<ReceiptSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ReceiptSequence s where s.receiptYear = :year")
    Optional<ReceiptSequence> findForUpdate(@Param("year") Integer year);
}
