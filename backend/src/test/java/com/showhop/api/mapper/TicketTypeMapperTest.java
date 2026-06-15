package com.showhop.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.showhop.api.dto.TicketTypeRequestDto;
import com.showhop.api.dto.TicketTypeResponseDto;
import com.showhop.api.entity.Event;
import com.showhop.api.entity.TicketType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketTypeMapperTest {

  private final TicketTypeMapper mapper = new TicketTypeMapperImpl();

  @Test
  void mapsARequestDtoToANewEntityLeavingTheEventUnset() {
    TicketTypeRequestDto dto =
        new TicketTypeRequestDto("General Admission", "Standard entry", new BigDecimal("29.99"), 200);

    TicketType ticketType = mapper.toEntity(dto);

    assertThat(ticketType.getId()).isNull();
    assertThat(ticketType.getEvent()).isNull();
    assertThat(ticketType.getName()).isEqualTo("General Admission");
    assertThat(ticketType.getPrice()).isEqualByComparingTo("29.99");
  }

  @Test
  void mapsAnEntityToAResponseDtoFlatteningTheEventToItsId() {
    UUID eventId = UUID.randomUUID();
    TicketType ticketType = TicketType.builder()
        .id(UUID.randomUUID())
        .event(Event.builder().id(eventId).build())
        .name("VIP")
        .price(new BigDecimal("99.00"))
        .totalAvailable(20)
        .build();

    TicketTypeResponseDto dto = mapper.toResponseDto(ticketType);

    assertThat(dto.eventId()).isEqualTo(eventId);
    assertThat(dto.totalAvailable()).isEqualTo(20);
  }
}
