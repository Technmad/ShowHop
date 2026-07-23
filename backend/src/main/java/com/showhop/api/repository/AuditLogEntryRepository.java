package com.showhop.api.repository;

import com.showhop.api.entity.AuditLogEntry;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, UUID> {

  Page<AuditLogEntry> findByOrganizerIdOrderByOccurredAtDesc(UUID organizerId, Pageable pageable);
}
