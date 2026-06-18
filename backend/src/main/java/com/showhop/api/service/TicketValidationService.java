package com.showhop.api.service;

import com.showhop.api.entity.TicketValidation;
import com.showhop.api.entity.enums.TicketValidationMethod;
import java.util.UUID;

public interface TicketValidationService {

  /**
   * Records one validation attempt. Always writes a row, even when the
   * outcome is INVALID -- an append-only log of every door-scan attempt,
   * not just the successful ones.
   */
  TicketValidation validateTicket(UUID staffId, UUID ticketId, TicketValidationMethod method);
}
