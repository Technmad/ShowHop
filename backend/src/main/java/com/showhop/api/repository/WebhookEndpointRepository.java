package com.showhop.api.repository;

import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

  Page<WebhookEndpoint> findByOrganizerId(UUID organizerId, Pageable pageable);

  Optional<WebhookEndpoint> findByIdAndOrganizerId(UUID id, UUID organizerId);

  List<WebhookEndpoint> findByOrganizerIdAndStatus(UUID organizerId, WebhookEndpointStatus status);
}
