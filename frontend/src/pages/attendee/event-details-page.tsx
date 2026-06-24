import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiRequest } from "../../lib/api";
import type { Page, PublishedEventResponse, TicketTypeResponse } from "../../domain/types";

export function EventDetailsPage() {
  const { eventId } = useParams<{ eventId: string }>();

  const [event, setEvent] = useState<PublishedEventResponse | null>(null);
  const [ticketTypes, setTicketTypes] = useState<TicketTypeResponse[]>([]);

  useEffect(() => {
    if (!eventId) return;
    apiRequest<PublishedEventResponse>(`/api/v1/published-events/${eventId}`).then(setEvent);
    apiRequest<Page<TicketTypeResponse>>(`/api/v1/published-events/${eventId}/ticket-types`).then(
      (page) => setTicketTypes(page.content),
    );
  }, [eventId]);

  if (!event) {
    return <p className="text-slate-600">Loading...</p>;
  }

  return (
    <section>
      <h1 className="text-3xl font-semibold tracking-tight">{event.name}</h1>
      <p className="mt-1 text-slate-600">{event.venue}</p>
      <p className="text-slate-600">{new Date(event.startsAt).toLocaleString()}</p>

      <ul className="mt-8 divide-y divide-slate-200">
        {ticketTypes.map((tt) => (
          <li key={tt.id} className="flex items-center justify-between py-4">
            <div>
              <p className="font-medium">{tt.name}</p>
              {tt.description && <p className="text-sm text-slate-600">{tt.description}</p>}
              <p className="text-sm text-slate-600">${tt.price}</p>
            </div>
            <Link
              to={`/events/${eventId}/ticket-types/${tt.id}/purchase`}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white"
            >
              Buy
            </Link>
          </li>
        ))}
        {ticketTypes.length === 0 && (
          <p className="py-4 text-slate-600">No ticket types available yet.</p>
        )}
      </ul>
    </section>
  );
}
