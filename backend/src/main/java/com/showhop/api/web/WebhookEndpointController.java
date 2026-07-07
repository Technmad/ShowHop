package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.WebhookEndpointPatchDto;
import com.showhop.api.dto.WebhookEndpointRequestDto;
import com.showhop.api.dto.WebhookEndpointResponseDto;
import com.showhop.api.mapper.WebhookEndpointMapper;
import com.showhop.api.service.WebhookEndpointService;
import com.showhop.api.service.WebhookEndpointService.WebhookEndpointPatchResult;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhook-endpoints")
@RequiredArgsConstructor
public class WebhookEndpointController {

  private final WebhookEndpointService webhookEndpointService;
  private final WebhookEndpointMapper webhookEndpointMapper;

  @PostMapping
  public ResponseEntity<WebhookEndpointResponseDto> registerEndpoint(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WebhookEndpointRequestDto request) {
    var endpoint = webhookEndpointService.registerEndpoint(parseUserId(jwt), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(webhookEndpointMapper.toResponseDtoWithSecret(endpoint));
  }

  @GetMapping
  public Page<WebhookEndpointResponseDto> listEndpoints(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    return webhookEndpointService.listEndpoints(parseUserId(jwt), pageable)
        .map(webhookEndpointMapper::toResponseDto);
  }

  @PatchMapping("/{endpointId}")
  public WebhookEndpointResponseDto patchEndpoint(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID endpointId,
      @RequestBody WebhookEndpointPatchDto request) {
    WebhookEndpointPatchResult result =
        webhookEndpointService.patchEndpoint(parseUserId(jwt), endpointId, request);
    return result.secretRotated()
        ? webhookEndpointMapper.toResponseDtoWithSecret(result.endpoint())
        : webhookEndpointMapper.toResponseDto(result.endpoint());
  }
}
