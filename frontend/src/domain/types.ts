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

export type WebhookEndpointStatus = "ACTIVE" | "DISABLED" | "CIRCUIT_OPEN";
export type WebhookDeliveryState = "PENDING" | "IN_FLIGHT" | "RETRYING" | "SUCCEEDED" | "DEAD_LETTER";
export type WebhookEventType = "EVENT_PUBLISHED" | "TICKET_PURCHASED" | "TICKET_VALIDATED";

/** Wire-format event type names, matching WebhookEventType.wireValue() on the backend. */
export const WEBHOOK_EVENT_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: "event.published", label: "Event published" },
  { value: "ticket.purchased", label: "Ticket purchased" },
  { value: "ticket.validated", label: "Ticket validated" },
];

export interface WebhookEndpointRequest {
  url: string;
  subscribedEventTypes: string[];
}

export interface WebhookEndpointPatchRequest {
  enabled?: boolean;
  subscribedEventTypes?: string[];
  rotateSecret?: boolean;
}

export interface WebhookEndpointResponse {
  id: string;
  url: string;
  subscribedEventTypes: string[];
  status: WebhookEndpointStatus;
  consecutiveFailures: number;
  circuitOpenedAt: string | null;
  /** Present only immediately after registration or a secret rotation. */
  secret: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookDeliveryResponse {
  id: string;
  eventType: WebhookEventType;
  state: WebhookDeliveryState;
  attempt: number;
  maxAttempts: number;
  probe: boolean;
  lastResponseCode: number | null;
  lastError: string | null;
  nextRetryAt: string | null;
  createdAt: string;
  updatedAt: string;
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
