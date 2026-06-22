import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { ApiError, apiRequest } from "../../lib/api";
import type { Page, PublishedEventResponse, TicketResponse, TicketTypeResponse } from "../../domain/types";

export function EventDetailsPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const auth = useAuth();
  const navigate = useNavigate();

  const [event, setEvent] = useState<PublishedEventResponse | null>(null);
  const [ticketTypes, setTicketTypes] = useState<TicketTypeResponse[]>([]);
  const [purchasing, setPurchasing] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!eventId) return;
    apiRequest<PublishedEventResponse>(`/api/v1/published-events/${eventId}`).then(setEvent);
    apiRequest<Page<TicketTypeResponse>>(`/api/v1/published-events/${eventId}/ticket-types`).then(
      (page) => setTicketTypes(page.content),
    );
  }, [eventId]);

  async function buy(ticketTypeId: string) {
    if (!eventId) return;

    if (!auth.isAuthenticated) {
      auth.signinRedirect();
      return;
    }

    setPurchasing(ticketTypeId);
    setError(null);
    try {
      const ticket = await apiRequest<TicketResponse>(
        `/api/v1/published-events/${eventId}/ticket-types/${ticketTypeId}/tickets`,
        { method: "POST", token: auth.user?.access_token },
      );
      navigate(`/tickets/${ticket.id}`);
    } catch (e) {
      setError(e instanceof ApiError && e.status === 409
        ? "That ticket type just sold out."
        : "Couldn't complete the purchase. Please try again.");
    } finally {
      setPurchasing(null);
    }
  }

  if (!event) {
    return <p className="text-slate-600">Loading...</p>;
  }

  return (
    <section>
      <h1 className="text-3xl font-semibold tracking-tight">{event.name}</h1>
      <p className="mt-1 text-slate-600">{event.venue}</p>
      <p className="text-slate-600">{new Date(event.startsAt).toLocaleString()}</p>

      {error && <p className="mt-4 text-red-600">{error}</p>}

      <ul className="mt-8 divide-y divide-slate-200">
        {ticketTypes.map((tt) => (
          <li key={tt.id} className="flex items-center justify-between py-4">
            <div>
              <p className="font-medium">{tt.name}</p>
              {tt.description && <p className="text-sm text-slate-600">{tt.description}</p>}
              <p className="text-sm text-slate-600">${tt.price}</p>
            </div>
            <button
              disabled={purchasing === tt.id}
              onClick={() => buy(tt.id)}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              {purchasing === tt.id ? "Buying..." : "Buy"}
            </button>
          </li>
        ))}
        {ticketTypes.length === 0 && (
          <p className="py-4 text-slate-600">No ticket types available yet.</p>
        )}
      </ul>
    </section>
  );
}
