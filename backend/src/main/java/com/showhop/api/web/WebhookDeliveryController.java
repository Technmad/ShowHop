package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.WebhookDeliveryResponseDto;
import com.showhop.api.mapper.WebhookDeliveryMapper;
import com.showhop.api.service.WebhookDeliveryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WebhookDeliveryController {

  private final WebhookDeliveryService webhookDeliveryService;
  private final WebhookDeliveryMapper webhookDeliveryMapper;

  @GetMapping("/webhook-endpoints/{endpointId}/deliveries")
  public Page<WebhookDeliveryResponseDto> listDeliveries(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID endpointId, Pageable pageable) {
    return webhookDeliveryService.listDeliveriesForEndpoint(parseUserId(jwt), endpointId, pageable)
        .map(webhookDeliveryMapper::toResponseDto);
  }

  @PostMapping("/webhook-deliveries/{deliveryId}/replay")
  public ResponseEntity<WebhookDeliveryResponseDto> replayDelivery(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID deliveryId) {
    var replay = webhookDeliveryService.replayDelivery(parseUserId(jwt), deliveryId);
    return ResponseEntity.status(HttpStatus.CREATED).body(webhookDeliveryMapper.toResponseDto(replay));
  }
}
