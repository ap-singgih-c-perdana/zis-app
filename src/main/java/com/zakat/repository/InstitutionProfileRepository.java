package com.zakat.repository;

import com.zakat.entity.InstitutionProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InstitutionProfileRepository extends JpaRepository<InstitutionProfile, UUID> {
}

