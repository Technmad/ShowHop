package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.ApiKeyRequestDto;
import com.showhop.api.dto.ApiKeyResponseDto;
import com.showhop.api.mapper.ApiKeyMapper;
import com.showhop.api.service.ApiKeyService;
import com.showhop.api.service.ApiKeyService.ApiKeyCreationResult;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

  private final ApiKeyService apiKeyService;
  private final ApiKeyMapper apiKeyMapper;

  @PostMapping
  public ResponseEntity<ApiKeyResponseDto> createKey(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ApiKeyRequestDto request) {
    ApiKeyCreationResult result = apiKeyService.createKey(parseUserId(jwt), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(apiKeyMapper.toResponseDtoWithKey(result.apiKey(), result.rawKey()));
  }

  @GetMapping
  public Page<ApiKeyResponseDto> listKeys(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    return apiKeyService.listKeys(parseUserId(jwt), pageable).map(apiKeyMapper::toResponseDto);
  }

  @DeleteMapping("/{keyId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeKey(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID keyId) {
    apiKeyService.revokeKey(parseUserId(jwt), keyId);
  }
}
