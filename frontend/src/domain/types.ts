export type EventStatus = "DRAFT" | "PUBLISHED" | "CANCELLED" | "COMPLETED";

export interface EventRequest {
  name: string;
  venue: string;
  startsAt: string;
  endsAt: string;
  salesStart: string | null;
  salesEnd: string | null;
  status: EventStatus;
}

export interface EventResponse {
  id: string;
  name: string;
  venue: string;
  startsAt: string;
  endsAt: string;
  salesStart: string | null;
  salesEnd: string | null;
  status: EventStatus;
  organizerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface PublishedEventResponse {
  id: string;
  name: string;
  venue: string;
  startsAt: string;
  endsAt: string;
}

export interface TicketTypeRequest {
  name: string;
  description: string | null;
  price: string;
  totalAvailable: number;
}

export interface TicketTypeResponse {
  id: string;
  eventId: string;
  name: string;
  description: string | null;
  price: string;
  totalAvailable: number;
  createdAt: string;
  updatedAt: string;
}

export type TicketStatus = "PURCHASED" | "CANCELLED";

export interface TicketResponse {
  id: string;
  ticketTypeId: string;
  eventId: string;
  status: TicketStatus;
  createdAt: string;
}

export type TicketValidationMethod = "QR_SCAN" | "MANUAL";
export type TicketValidationStatus = "VALID" | "INVALID" | "EXPIRED";

export interface TicketValidationRequest {
  ticketId: string;
  method: TicketValidationMethod;
}

export interface TicketValidationResponse {
  id: string;
  ticketId: string;
  status: TicketValidationStatus;
  method: TicketValidationMethod;
  validatedAt: string;
  validatedById: string;
}

export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
