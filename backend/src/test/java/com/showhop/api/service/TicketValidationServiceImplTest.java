package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.TicketStatus;
import com.showhop.api.entity.enums.TicketValidationMethod;
import com.showhop.api.entity.enums.TicketValidationStatus;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.repository.TicketValidationRepository;
import com.showhop.api.repository.UserRepository;
import com.showhop.api.service.impl.TicketValidationServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketValidationServiceImplTest {

  @Mock
  private TicketRepository ticketRepository;
  @Mock
  private TicketValidationRepository ticketValidationRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private TicketValidationServiceImpl ticketValidationService;

  @Test
  void firstScanOfAPurchasedTicketIsValid() {
    UUID staffId = UUID.randomUUID();
    UUID ticketId = UUID.randomUUID();
    setUpTicketAndStaff(ticketId, staffId, TicketStatus.PURCHASED);
    when(ticketValidationRepository.existsByTicketIdAndStatus(ticketId, TicketValidationStatus.VALID))
        .thenReturn(false);
    when(ticketValidationRepository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var validation = ticketValidationService.validateTicket(
        staffId, ticketId, TicketValidationMethod.QR_SCAN);

    assertThat(validation.getStatus()).isEqualTo(TicketValidationStatus.VALID);
  }

  @Test
  void aSecondScanOfAnAlreadyAdmittedTicketIsInvalid() {
    UUID staffId = UUID.randomUUID();
    UUID ticketId = UUID.randomUUID();
    setUpTicketAndStaff(ticketId, staffId, TicketStatus.PURCHASED);
    when(ticketValidationRepository.existsByTicketIdAndStatus(ticketId, TicketValidationStatus.VALID))
        .thenReturn(true); // already scanned in once
    when(ticketValidationRepository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var validation = ticketValidationService.validateTicket(
        staffId, ticketId, TicketValidationMethod.QR_SCAN);

    assertThat(validation.getStatus()).isEqualTo(TicketValidationStatus.INVALID);
  }

  @Test
  void aCancelledTicketIsAlwaysInvalidRegardlessOfPriorScans() {
    UUID staffId = UUID.randomUUID();
    UUID ticketId = UUID.randomUUID();
    setUpTicketAndStaff(ticketId, staffId, TicketStatus.CANCELLED);
    when(ticketValidationRepository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var validation = ticketValidationService.validateTicket(
        staffId, ticketId, TicketValidationMethod.MANUAL);

    assertThat(validation.getStatus()).isEqualTo(TicketValidationStatus.INVALID);
  }

  private void setUpTicketAndStaff(UUID ticketId, UUID staffId, TicketStatus status) {
    Ticket ticket = Ticket.builder().id(ticketId).status(status).build();
    User staff = User.builder().id(staffId).build();
    when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
    when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
  }
}
