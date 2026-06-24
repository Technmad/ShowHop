import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { ApiError, apiRequest } from "../../lib/api";
import type { PublishedEventResponse, TicketResponse, TicketTypeResponse } from "../../domain/types";

/**
 * A confirm-and-pay step between "choose a ticket type" and "you have a
 * ticket" -- matching devtiro's checkout page. There's no real payment
 * processor behind this yet (see docs/PRD.md roadmap, phase 3), so the
 * card fields are for-show only and aren't sent anywhere; the actual
 * purchase call is identical to what a direct "Buy" button would do.
 */
export function PurchasePage() {
  const { eventId, ticketTypeId } = useParams<{ eventId: string; ticketTypeId: string }>();
  const auth = useAuth();
  const navigate = useNavigate();

  const [event, setEvent] = useState<PublishedEventResponse | null>(null);
  const [ticketType, setTicketType] = useState<TicketTypeResponse | null>(null);
  const [cardNumber, setCardNumber] = useState("");
  const [cardholderName, setCardholderName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!eventId || !ticketTypeId) return;
    apiRequest<PublishedEventResponse>(`/api/v1/published-events/${eventId}`).then(setEvent);
    apiRequest<{ content: TicketTypeResponse[] }>(
      `/api/v1/published-events/${eventId}/ticket-types`,
    ).then((page) => setTicketType(page.content.find((tt) => tt.id === ticketTypeId) ?? null));
  }, [eventId, ticketTypeId]);

  async function handleConfirm(e: React.FormEvent) {
    e.preventDefault();
    if (!eventId || !ticketTypeId) return;

    setSubmitting(true);
    setError(null);
    try {
      const ticket = await apiRequest<TicketResponse>(
        `/api/v1/published-events/${eventId}/ticket-types/${ticketTypeId}/tickets`,
        { method: "POST", token: auth.user?.access_token },
      );
      navigate(`/tickets/${ticket.id}`);
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409
        ? "That ticket type just sold out."
        : "Couldn't complete the purchase. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!event || !ticketType) {
    return <p className="text-slate-600">Loading...</p>;
  }

  return (
    <section className="max-w-sm">
      <h1 className="text-2xl font-semibold tracking-tight">Confirm your order</h1>

      <div className="mt-4 rounded border border-slate-200 p-4">
        <p className="font-medium">{event.name}</p>
        <p className="text-sm text-slate-600">{ticketType.name}</p>
        <p className="mt-2 text-lg font-semibold">${ticketType.price}</p>
      </div>

      <form onSubmit={handleConfirm} className="mt-6 space-y-4">
        <div>
          <label className="block text-sm font-medium">Card number</label>
          <input
            required
            inputMode="numeric"
            placeholder="4242 4242 4242 4242"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
            value={cardNumber}
            onChange={(e) => setCardNumber(e.target.value)}
          />
        </div>
        <div>
          <label className="block text-sm font-medium">Cardholder name</label>
          <input
            required
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
            value={cardholderName}
            onChange={(e) => setCardholderName(e.target.value)}
          />
        </div>

        {error && <p className="text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {submitting ? "Processing..." : `Pay $${ticketType.price}`}
        </button>
      </form>
    </section>
  );
}
