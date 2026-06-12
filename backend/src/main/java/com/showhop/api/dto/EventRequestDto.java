package com.showhop.api.dto;

import com.showhop.api.entity.enums.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record EventRequestDto(
    @NotBlank String name,
    @NotBlank String venue,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    Instant salesStart,
    Instant salesEnd,
    @NotNull EventStatus status) {
}
