package com.showhop.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ApiKeyRequestDto(@NotBlank String name) {
}
