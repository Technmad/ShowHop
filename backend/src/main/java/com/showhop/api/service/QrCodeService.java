package com.showhop.api.service;

import com.showhop.api.entity.QrCode;
import com.showhop.api.entity.Ticket;
import java.util.UUID;

public interface QrCodeService {

  /** Generates and persists a QR code for a newly purchased ticket. */
  QrCode generateQrCode(Ticket ticket);

  /**
   * Renders the PNG image for a ticket the given user actually purchased.
   *
   * @throws com.showhop.api.exception.TicketNotFoundException if the ticket
   *     doesn't exist or doesn't belong to that user
   * @throws com.showhop.api.exception.QrCodeNotFoundException if the ticket
   *     has no QR code (shouldn't happen for a ticket bought through the
   *     normal purchase flow, but the check exists rather than NPE-ing)
   */
  byte[] getQrCodeImageForUserAndTicket(UUID userId, UUID ticketId);
}
