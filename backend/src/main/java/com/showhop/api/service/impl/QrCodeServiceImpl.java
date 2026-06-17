package com.showhop.api.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.showhop.api.entity.QrCode;
import com.showhop.api.entity.Ticket;
import com.showhop.api.entity.enums.QrCodeStatus;
import com.showhop.api.exception.QrCodeGenerationException;
import com.showhop.api.exception.QrCodeNotFoundException;
import com.showhop.api.exception.TicketNotFoundException;
import com.showhop.api.repository.QrCodeRepository;
import com.showhop.api.repository.TicketRepository;
import com.showhop.api.service.QrCodeService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

  private static final int QR_SIZE_PX = 300;

  private final QrCodeRepository qrCodeRepository;
  private final TicketRepository ticketRepository;

  @Override
  @Transactional
  public QrCode generateQrCode(Ticket ticket) {
    QrCode qrCode = QrCode.builder()
        .ticket(ticket)
        .status(QrCodeStatus.ACTIVE)
        .generatedAt(Instant.now())
        .build();

    return qrCodeRepository.save(qrCode);
  }

  @Override
  public byte[] getQrCodeImageForUserAndTicket(UUID userId, UUID ticketId) {
    Ticket ticket = ticketRepository.findByIdAndPurchaserId(ticketId, userId)
        .orElseThrow(() -> new TicketNotFoundException(
            "Ticket with id '%s' was not found".formatted(ticketId)));

    QrCode qrCode = qrCodeRepository.findByTicketId(ticket.getId())
        .orElseThrow(() -> new QrCodeNotFoundException(
            "Ticket '%s' has no QR code".formatted(ticketId)));

    return renderPng(qrCode.getTicket().getId());
  }

  private byte[] renderPng(UUID ticketId) {
    try {
      BitMatrix matrix = new QRCodeWriter().encode(
          ticketId.toString(), BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return out.toByteArray();
    } catch (WriterException | IOException e) {
      throw new QrCodeGenerationException(
          "Failed to render QR code for ticket '%s'".formatted(ticketId), e);
    }
  }
}
