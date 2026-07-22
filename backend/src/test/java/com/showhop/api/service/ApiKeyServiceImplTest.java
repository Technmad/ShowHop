package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

  @InjectMocks
  private ApiKeyServiceImpl apiKeyService;

  @Test
  void creatingAKeyReturnsARawKeyWhoseHashMatchesTheStoredEntity() {
    UUID organizerId = UUID.randomUUID();
    when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ApiKeyCreationResult result = apiKeyService.createKey(organizerId, new ApiKeyRequestDto("CI integration"));

    assertThat(result.rawKey()).startsWith(ApiKeyHasher.KEY_MARKER);
    assertThat(result.apiKey().getOrganizerId()).isEqualTo(organizerId);
    assertThat(result.apiKey().getName()).isEqualTo("CI integration");
    assertThat(result.apiKey().getHashedKey()).isEqualTo(ApiKeyHasher.hash(result.rawKey()));
    assertThat(result.apiKey().getKeyPrefix()).isEqualTo(ApiKeyHasher.prefixOf(result.rawKey()));
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
        .organizerId(organizerId).name("CI").keyPrefix("abcd1234").hashedKey("hashed").build();
    when(apiKeyRepository.findByIdAndOrganizerId(apiKey.getId(), organizerId))
        .thenReturn(Optional.of(apiKey));

    apiKeyService.revokeKey(organizerId, apiKey.getId());

    assertThat(apiKey.getRevokedAt()).isNotNull();
  }
}
