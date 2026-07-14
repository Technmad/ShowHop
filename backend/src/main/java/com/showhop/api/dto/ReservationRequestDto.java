package com.showhop.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReservationRequestDto(@NotNull @Min(1) Integer quantity) {
}
