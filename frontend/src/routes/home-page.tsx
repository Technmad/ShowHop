import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest } from "../lib/api";
import type { Page, PublishedEventResponse } from "../domain/types";

export function HomePage() {
  const [query, setQuery] = useState("");
  const [events, setEvents] = useState<PublishedEventResponse[] | null>(null);

  useEffect(() => {
    const params = query.trim() ? `?q=${encodeURIComponent(query.trim())}` : "";
    apiRequest<Page<PublishedEventResponse>>(`/api/v1/published-events${params}`)
      .then((page) => setEvents(page.content))
      .catch(() => setEvents([]));
  }, [query]);

  return (
    <section>
      <h1 className="text-3xl font-semibold tracking-tight">Find your next event</h1>
      <input
        type="search"
        placeholder="Search by name or venue"
        className="mt-4 w-full max-w-md rounded border border-slate-300 px-3 py-2 text-sm"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />

      {events && events.length === 0 && (
        <p className="mt-6 text-slate-600">No events found.</p>
      )}

      <ul className="mt-6 grid gap-4 sm:grid-cols-2">
        {events?.map((event) => (
          <li key={event.id} className="rounded border border-slate-200 p-4">
            <Link to={`/events/${event.id}`} className="font-medium hover:underline">
              {event.name}
            </Link>
            <p className="mt-1 text-sm text-slate-600">{event.venue}</p>
            <p className="text-sm text-slate-600">
              {new Date(event.startsAt).toLocaleString()}
            </p>
          </li>
        ))}
      </ul>
    </section>
  );
}
