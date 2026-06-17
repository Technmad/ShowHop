package com.showhop.api.service;

import com.showhop.api.entity.Ticket;
import java.util.UUID;

public interface TicketPurchaseService {

  /**
   * Buys one ticket of the given type on behalf of the given user.
   *
   * @throws com.showhop.api.exception.EventNotFoundException if the ticket
   *     type doesn't belong to a published event with that id
   * @throws com.showhop.api.exception.TicketsSoldOutException if capacity
   *     is already exhausted
   */
  Ticket purchaseTicket(UUID buyerId, UUID eventId, UUID ticketTypeId);
}
