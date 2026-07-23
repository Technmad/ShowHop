package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.showhop.api.dto.WebhookEndpointPatchDto;
import com.showhop.api.dto.WebhookEndpointRequestDto;
import com.showhop.api.entity.WebhookEndpoint;
import com.showhop.api.entity.enums.WebhookEndpointStatus;
import com.showhop.api.exception.WebhookEndpointNotFoundException;
import com.showhop.api.repository.WebhookEndpointRepository;
import com.showhop.api.service.impl.WebhookEndpointServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookEndpointServiceImplTest {

  @Mock
  private WebhookEndpointRepository webhookEndpointRepository;

  @Mock
  private AuditLogService auditLogService;

  @InjectMocks
  private WebhookEndpointServiceImpl webhookEndpointService;

  @Test
  void registeringAnEndpointGeneratesASecretAndDefaultsToActive() {
    UUID organizerId = UUID.randomUUID();
    WebhookEndpointRequestDto request =
        new WebhookEndpointRequestDto("https://example.com/hooks", List.of("event.published"));
    when(webhookEndpointRepository.save(org.mockito.ArgumentMatchers.any(WebhookEndpoint.class)))
        .thenAnswer(invocation -> {
          WebhookEndpoint endpoint = invocation.getArgument(0);
          endpoint.setId(UUID.randomUUID()); // simulates @GeneratedValue on insert
          return endpoint;
        });

    WebhookEndpoint saved = webhookEndpointService.registerEndpoint(organizerId, request);

    assertThat(saved.getOrganizerId()).isEqualTo(organizerId);
    assertThat(saved.getStatus()).isEqualTo(WebhookEndpointStatus.ACTIVE);
    assertThat(saved.getSecret()).startsWith("whsec_");
    org.mockito.Mockito.verify(auditLogService).record(
        org.mockito.ArgumentMatchers.eq(organizerId), org.mockito.ArgumentMatchers.eq(organizerId),
        org.mockito.ArgumentMatchers.eq("WEBHOOK_ENDPOINT_CREATED"), org.mockito.ArgumentMatchers.eq("WebhookEndpoint"),
        org.mockito.ArgumentMatchers.eq(saved.getId().toString()), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void patchingRejectsAnEndpointTheOrganizerDoesNotOwn() {
    UUID organizerId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    when(webhookEndpointRepository.findByIdAndOrganizerId(endpointId, organizerId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> webhookEndpointService.patchEndpoint(
        organizerId, endpointId, new WebhookEndpointPatchDto(null, null, null)))
        .isInstanceOf(WebhookEndpointNotFoundException.class);
  }

  @Test
  void disablingAnEndpointDoesNotResetItsFailureCount() {
    UUID organizerId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    WebhookEndpoint endpoint = anEndpoint(organizerId, endpointId, WebhookEndpointStatus.ACTIVE, 3);
    when(webhookEndpointRepository.findByIdAndOrganizerId(endpointId, organizerId))
        .thenReturn(Optional.of(endpoint));
    when(webhookEndpointRepository.save(endpoint)).thenReturn(endpoint);

    var result = webhookEndpointService.patchEndpoint(
        organizerId, endpointId, new WebhookEndpointPatchDto(false, null, null));

    assertThat(result.endpoint().getStatus()).isEqualTo(WebhookEndpointStatus.DISABLED);
    assertThat(result.endpoint().getConsecutiveFailures()).isEqualTo(3);
    assertThat(result.secretRotated()).isFalse();
  }

  @Test
  void reEnablingAnEndpointResetsFailuresAndClearsTheCircuitBreaker() {
    UUID organizerId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    WebhookEndpoint endpoint = anEndpoint(organizerId, endpointId, WebhookEndpointStatus.CIRCUIT_OPEN, 5);
    endpoint.setCircuitOpenedAt(Instant.now());
    when(webhookEndpointRepository.findByIdAndOrganizerId(endpointId, organizerId))
        .thenReturn(Optional.of(endpoint));
    when(webhookEndpointRepository.save(endpoint)).thenReturn(endpoint);

    var result = webhookEndpointService.patchEndpoint(
        organizerId, endpointId, new WebhookEndpointPatchDto(true, null, null));

    assertThat(result.endpoint().getStatus()).isEqualTo(WebhookEndpointStatus.ACTIVE);
    assertThat(result.endpoint().getConsecutiveFailures()).isZero();
    assertThat(result.endpoint().getCircuitOpenedAt()).isNull();
  }

  @Test
  void rotatingTheSecretChangesItAndReportsRotationHappened() {
    UUID organizerId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    WebhookEndpoint endpoint = anEndpoint(organizerId, endpointId, WebhookEndpointStatus.ACTIVE, 0);
    String oldSecret = endpoint.getSecret();
    when(webhookEndpointRepository.findByIdAndOrganizerId(endpointId, organizerId))
        .thenReturn(Optional.of(endpoint));
    when(webhookEndpointRepository.save(endpoint)).thenReturn(endpoint);

    var result = webhookEndpointService.patchEndpoint(
        organizerId, endpointId, new WebhookEndpointPatchDto(null, null, true));

    assertThat(result.secretRotated()).isTrue();
    assertThat(result.endpoint().getSecret()).isNotEqualTo(oldSecret).startsWith("whsec_");
    org.mockito.Mockito.verify(auditLogService).record(
        org.mockito.ArgumentMatchers.eq(organizerId), org.mockito.ArgumentMatchers.eq(organizerId),
        org.mockito.ArgumentMatchers.eq("WEBHOOK_ENDPOINT_SECRET_ROTATED"),
        org.mockito.ArgumentMatchers.eq("WebhookEndpoint"), org.mockito.ArgumentMatchers.eq(endpointId.toString()),
        org.mockito.ArgumentMatchers.isNull());
  }

  private WebhookEndpoint anEndpoint(
      UUID organizerId, UUID endpointId, WebhookEndpointStatus status, int consecutiveFailures) {
    return WebhookEndpoint.builder()
        .id(endpointId).organizerId(organizerId).url("https://example.com/hooks")
        .secret("whsec_original").subscribedEventTypes(List.of("event.published"))
        .status(status).consecutiveFailures(consecutiveFailures).build();
  }
}
