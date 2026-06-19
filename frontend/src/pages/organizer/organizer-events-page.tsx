import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest } from "../../lib/api";
import { useAuth } from "react-oidc-context";
import type { EventResponse, Page } from "../../domain/types";

export function OrganizerEventsPage() {
  const auth = useAuth();
  const [events, setEvents] = useState<EventResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!auth.user?.access_token) return;
    apiRequest<Page<EventResponse>>("/api/v1/events", { token: auth.user.access_token })
      .then((page) => setEvents(page.content))
      .catch(() => setError("Couldn't load your events."));
  }, [auth.user?.access_token]);

  return (
    <section>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Your events</h1>
        <Link
          to="/organizer/events/new"
          className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white"
        >
          New event
        </Link>
      </div>

      {error && <p className="mt-4 text-red-600">{error}</p>}

      {events && events.length === 0 && (
        <p className="mt-6 text-slate-600">You haven't created any events yet.</p>
      )}

      <ul className="mt-6 divide-y divide-slate-200">
        {events?.map((event) => (
          <li key={event.id} className="flex items-center justify-between py-4">
            <div>
              <p className="font-medium">{event.name}</p>
              <p className="text-sm text-slate-600">
                {event.venue} · {event.status}
              </p>
            </div>
            <Link to={`/organizer/events/${event.id}`} className="text-sm underline">
              Manage
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
