package com.showhop.api.mapper;

import com.showhop.api.dto.ReservationInitiationResponseDto;
import com.showhop.api.dto.ReservationStatusDto;
import com.showhop.api.entity.TicketReservation;
import com.showhop.api.service.ReservationService.ReservationInitiationResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

  /** {@code razorpayKeyId} isn't stored on the entity -- it's the public key, assembled by the caller. */
  default ReservationInitiationResponseDto toInitiationResponseDto(
      ReservationInitiationResult result, String razorpayKeyId) {
    TicketReservation reservation = result.reservation();
    return new ReservationInitiationResponseDto(
        reservation.getId(),
        reservation.getState(),
        reservation.getExpiresAt(),
        reservation.getRazorpayOrderId(),
        razorpayKeyId,
        result.amountInPaise());
  }

  ReservationStatusDto toStatusDto(TicketReservation reservation);
}
