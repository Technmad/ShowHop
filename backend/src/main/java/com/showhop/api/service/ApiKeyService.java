package com.showhop.api.service;

import com.showhop.api.dto.ApiKeyRequestDto;
import com.showhop.api.entity.ApiKey;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApiKeyService {

  /** {@code rawKey} is the only time the plaintext key is ever available -- it isn't persisted. */
  record ApiKeyCreationResult(ApiKey apiKey, String rawKey) {
  }

  ApiKeyCreationResult createKey(UUID organizerId, ApiKeyRequestDto request);

  Page<ApiKey> listKeys(UUID organizerId, Pageable pageable);

  void revokeKey(UUID organizerId, UUID keyId);
}
