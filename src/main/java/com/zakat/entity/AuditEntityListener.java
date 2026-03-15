package com.zakat.entity;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

public class AuditEntityListener {

    @PrePersist
    public void prePersist(Object target) {
        if (!(target instanceof AuditableEntity auditable)) {
            return;
        }
        Instant now = Instant.now();
        String actor = currentActor();
        if (auditable.getCreatedAt() == null) {
            auditable.setCreatedAt(now);
        }
        if (auditable.getCreatedBy() == null) {
            auditable.setCreatedBy(actor);
        }
        auditable.setUpdatedAt(now);
        auditable.setUpdatedBy(actor);
    }

    @PreUpdate
    public void preUpdate(Object target) {
        if (!(target instanceof AuditableEntity auditable)) {
            return;
        }
        auditable.setUpdatedAt(Instant.now());
        auditable.setUpdatedBy(currentActor());
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }
}
