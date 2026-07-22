package com.showhop.api.repository;

import com.showhop.api.entity.ApiKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

  Page<ApiKey> findByOrganizerId(UUID organizerId, Pageable pageable);

  Optional<ApiKey> findByIdAndOrganizerId(UUID id, UUID organizerId);

  Optional<ApiKey> findByKeyPrefixAndRevokedAtIsNull(String keyPrefix);
}
