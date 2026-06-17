package com.showhop.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.showhop.api.entity.QrCode;
import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.User;
import com.showhop.api.entity.enums.QrCodeStatus;
import com.showhop.api.exception.QrCodeNotFoundException;
import com.showhop.api.exception.TicketNotFoundException;
import com.showhop.api.repository.QrCodeRepository;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.service.impl.QrCodeServiceImpl;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceImplTest {

  @Mock
  private QrCodeRepository qrCodeRepository;
  @Mock
  private TicketRepository ticketRepository;

  @InjectMocks
  private QrCodeServiceImpl qrCodeService;

  @Test
  void generateQrCodePersistsAnActiveCodeForTheTicket() {
    Ticket ticket = Ticket.builder().id(UUID.randomUUID()).build();
    when(qrCodeRepository.save(org.mockito.ArgumentMatchers.any(QrCode.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    QrCode qrCode = qrCodeService.generateQrCode(ticket);

    assertThat(qrCode.getStatus()).isEqualTo(QrCodeStatus.ACTIVE);
    assertThat(qrCode.getTicket()).isSameAs(ticket);
    assertThat(qrCode.getGeneratedAt()).isNotNull();
  }

  @Test
  void rendersARealDecodablePngForTheOwningUsersTicket() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID ticketId = UUID.randomUUID();
    Ticket ticket = Ticket.builder()
        .id(ticketId).purchaser(User.builder().id(userId).build()).build();

    when(ticketRepository.findByIdAndPurchaserId(ticketId, userId))
        .thenReturn(Optional.of(ticket));
    when(qrCodeRepository.findByTicketId(ticketId))
        .thenReturn(Optional.of(QrCode.builder().ticket(ticket).status(QrCodeStatus.ACTIVE).build()));

    byte[] png = qrCodeService.getQrCodeImageForUserAndTicket(userId, ticketId);

    assertThat(png).isNotEmpty();
    assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull(); // a real, decodable PNG
  }

  @Test
  void rejectsAccessToATicketTheUserDidNotPurchase() {
    UUID userId = UUID.randomUUID();
    UUID ticketId = UUID.randomUUID();
    when(ticketRepository.findByIdAndPurchaserId(ticketId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> qrCodeService.getQrCodeImageForUserAndTicket(userId, ticketId))
        .isInstanceOf(TicketNotFoundException.class);
  }

  @Test
  void surfacesAMissingQrCodeRatherThanThrowingAnNpe() {
    UUID userId = UUID.randomUUID();
    UUID ticketId = UUID.randomUUID();
    Ticket ticket = Ticket.builder().id(ticketId).build();
    when(ticketRepository.findByIdAndPurchaserId(ticketId, userId))
        .thenReturn(Optional.of(ticket));
    when(qrCodeRepository.findByTicketId(ticketId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> qrCodeService.getQrCodeImageForUserAndTicket(userId, ticketId))
        .isInstanceOf(QrCodeNotFoundException.class);
  }
}
