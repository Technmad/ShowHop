package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.showhop.api.dto.ApiKeyRequestDto;
import com.showhop.api.entity.ApiKey;
import com.showhop.api.exception.ApiKeyNotFoundException;
import com.showhop.api.repository.ApiKeyRepository;
import com.showhop.api.security.ApiKeyHasher;
import com.showhop.api.service.ApiKeyService.ApiKeyCreationResult;
import com.showhop.api.service.impl.ApiKeyServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceImplTest {

  @Mock
  private ApiKeyRepository apiKeyRepository;

  @Mock
  private AuditLogService auditLogService;

  @InjectMocks
  private ApiKeyServiceImpl apiKeyService;

  @Test
  void creatingAKeyReturnsARawKeyWhoseHashMatchesTheStoredEntity() {
    UUID organizerId = UUID.randomUUID();
    when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
      ApiKey saved = invocation.getArgument(0);
      saved.setId(UUID.randomUUID()); // simulates @GeneratedValue on insert
      return saved;
    });

    ApiKeyCreationResult result = apiKeyService.createKey(organizerId, new ApiKeyRequestDto("CI integration"));

    assertThat(result.rawKey()).startsWith(ApiKeyHasher.KEY_MARKER);
    assertThat(result.apiKey().getOrganizerId()).isEqualTo(organizerId);
    assertThat(result.apiKey().getName()).isEqualTo("CI integration");
    assertThat(result.apiKey().getHashedKey()).isEqualTo(ApiKeyHasher.hash(result.rawKey()));
    assertThat(result.apiKey().getKeyPrefix()).isEqualTo(ApiKeyHasher.prefixOf(result.rawKey()));
    verify(auditLogService).record(
        eq(organizerId), eq(organizerId), eq("API_KEY_CREATED"), eq("ApiKey"),
        eq(result.apiKey().getId().toString()), any());
  }

  @Test
  void revokingAForeignOrAlreadyGoneKeyThrowsNotFound() {
    UUID organizerId = UUID.randomUUID();
    UUID keyId = UUID.randomUUID();
    when(apiKeyRepository.findByIdAndOrganizerId(keyId, organizerId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> apiKeyService.revokeKey(organizerId, keyId))
        .isInstanceOf(ApiKeyNotFoundException.class);
  }

  @Test
  void revokingAnOwnedKeyStampsRevokedAt() {
    UUID organizerId = UUID.randomUUID();
    ApiKey apiKey = ApiKey.builder()
        .id(UUID.randomUUID()).organizerId(organizerId).name("CI").keyPrefix("abcd1234").hashedKey("hashed").build();
    when(apiKeyRepository.findByIdAndOrganizerId(apiKey.getId(), organizerId))
        .thenReturn(Optional.of(apiKey));

    apiKeyService.revokeKey(organizerId, apiKey.getId());

    assertThat(apiKey.getRevokedAt()).isNotNull();
    verify(auditLogService).record(
        eq(organizerId), eq(organizerId), eq("API_KEY_REVOKED"), eq("ApiKey"),
        eq(apiKey.getId().toString()), any());
  }
}
