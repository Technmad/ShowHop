package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.showhop.api.entity.Event;
import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.TicketType;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.EventStatus;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.exception.EventNotFoundException;
import com.showhop.api.exception.TicketsSoldOutException;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketTypeRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.impl.TicketPurchaseServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketPurchaseServiceImplTest {

  @Mock
  private TicketTypeRepository ticketTypeRepository;
  @Mock
  private TicketRepository ticketRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private QrCodeService qrCodeService;

  @InjectMocks
  private TicketPurchaseServiceImpl ticketPurchaseService;

  @Test
  void purchasesATicketWhenCapacityRemains() {
    UUID buyerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    User buyer = User.builder().id(buyerId).build();
    TicketType ticketType = aPublishedTicketType(eventId, ticketTypeId, 10);

    when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
    when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
    when(ticketRepository.countByTicketTypeIdAndStatus(ticketTypeId, TicketStatus.PURCHASED))
        .thenReturn(9);
    when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Ticket ticket = ticketPurchaseService.purchaseTicket(buyerId, eventId, ticketTypeId);

    assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PURCHASED);
    assertThat(ticket.getPurchaser()).isSameAs(buyer);
  }

  @Test
  void rejectsAPurchaseThatWouldExceedCapacity() {
    UUID buyerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    TicketType ticketType = aPublishedTicketType(eventId, ticketTypeId, 10);

    when(userRepository.findById(buyerId)).thenReturn(Optional.of(User.builder().id(buyerId).build()));
    when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
    when(ticketRepository.countByTicketTypeIdAndStatus(ticketTypeId, TicketStatus.PURCHASED))
        .thenReturn(10);

    assertThatThrownBy(() -> ticketPurchaseService.purchaseTicket(buyerId, eventId, ticketTypeId))
        .isInstanceOf(TicketsSoldOutException.class);
  }

  @Test
  void rejectsAPurchaseForATicketTypeThatDoesNotBelongToThatEvent() {
    UUID buyerId = UUID.randomUUID();
    UUID wrongEventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    TicketType ticketType = aPublishedTicketType(UUID.randomUUID(), ticketTypeId, 10);

    when(userRepository.findById(buyerId)).thenReturn(Optional.of(User.builder().id(buyerId).build()));
    when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));

    assertThatThrownBy(
        () -> ticketPurchaseService.purchaseTicket(buyerId, wrongEventId, ticketTypeId))
        .isInstanceOf(EventNotFoundException.class);
  }

  @Test
  void rejectsAPurchaseForAnUnpublishedEvent() {
    UUID buyerId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID ticketTypeId = UUID.randomUUID();
    Event draftEvent = Event.builder().id(eventId).status(EventStatus.DRAFT).build();
    TicketType ticketType = TicketType.builder()
        .id(ticketTypeId).event(draftEvent).totalAvailable(10).build();

    when(userRepository.findById(buyerId)).thenReturn(Optional.of(User.builder().id(buyerId).build()));
    when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));

    assertThatThrownBy(() -> ticketPurchaseService.purchaseTicket(buyerId, eventId, ticketTypeId))
        .isInstanceOf(EventNotFoundException.class);
  }

  private TicketType aPublishedTicketType(UUID eventId, UUID ticketTypeId, int totalAvailable) {
    Event event = Event.builder().id(eventId).status(EventStatus.PUBLISHED).build();
    return TicketType.builder()
        .id(ticketTypeId)
        .event(event)
        .totalAvailable(totalAvailable)
        .build();
  }
}
