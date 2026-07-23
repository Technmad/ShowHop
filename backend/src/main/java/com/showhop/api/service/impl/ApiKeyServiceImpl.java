package com.showhop.api.service.impl;

import com.showhop.api.dto.ApiKeyRequestDto;
import com.showhop.api.entity.ApiKey;
import com.showhop.api.exception.ApiKeyNotFoundException;
import com.showhop.api.repository.ApiKeyRepository;
import com.showhop.api.security.ApiKeyHasher;
import com.showhop.api.service.ApiKeyService;
import com.showhop.api.service.AuditLogService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

  private final ApiKeyRepository apiKeyRepository;
  private final AuditLogService auditLogService;
  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  @Transactional
  public ApiKeyCreationResult createKey(UUID organizerId, ApiKeyRequestDto request) {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String rawKey = ApiKeyHasher.KEY_MARKER + secret;

    ApiKey apiKey = apiKeyRepository.save(ApiKey.builder()
        .organizerId(organizerId)
        .name(request.name())
        .keyPrefix(ApiKeyHasher.prefixOf(rawKey))
        .hashedKey(ApiKeyHasher.hash(rawKey))
        .build());
    auditLogService.record(organizerId, organizerId, "API_KEY_CREATED", "ApiKey",
        apiKey.getId().toString(), Map.of("name", apiKey.getName()));

    return new ApiKeyCreationResult(apiKey, rawKey);
  }

  @Override
  public Page<ApiKey> listKeys(UUID organizerId, Pageable pageable) {
    return apiKeyRepository.findByOrganizerId(organizerId, pageable);
  }

  @Override
  @Transactional
  public void revokeKey(UUID organizerId, UUID keyId) {
    ApiKey apiKey = apiKeyRepository.findByIdAndOrganizerId(keyId, organizerId)
        .orElseThrow(() -> new ApiKeyNotFoundException(
            "API key with id '%s' was not found".formatted(keyId)));
    apiKey.setRevokedAt(Instant.now());
    auditLogService.record(organizerId, organizerId, "API_KEY_REVOKED", "ApiKey",
        apiKey.getId().toString(), null);
  }
}
