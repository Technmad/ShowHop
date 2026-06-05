package com.showhop.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Common audit timestamps for every entity. Stamped manually rather than via
 * Spring Data JPA auditing so entities have no dependency on an
 * {@code AuditorAware}/{@code @EnableJpaAuditing} setup they don't need yet.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
