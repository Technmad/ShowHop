package com.showhop.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record WebhookEndpointRequestDto(
    @NotBlank String url,
    @NotEmpty List<String> subscribedEventTypes) {
}
