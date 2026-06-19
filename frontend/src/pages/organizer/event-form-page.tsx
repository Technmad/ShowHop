import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { apiRequest } from "../../lib/api";
import { useAuth } from "react-oidc-context";
import type { EventRequest, EventResponse, EventStatus } from "../../domain/types";
import { EventTicketTypes } from "./event-ticket-types";

const EMPTY_FORM = {
  name: "",
  venue: "",
  startsAt: "",
  endsAt: "",
  status: "DRAFT" as EventStatus,
};

function toInputValue(iso: string | null): string {
  return iso ? iso.slice(0, 16) : "";
}

function toIso(inputValue: string): string {
  return new Date(inputValue).toISOString();
}

export function EventFormPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const isEditing = eventId !== undefined && eventId !== "new";
  const auth = useAuth();
  const token = auth.user?.access_token;
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isEditing || !token) return;
    apiRequest<EventResponse>(`/api/v1/events/${eventId}`, { token }).then((event) => {
      setForm({
        name: event.name,
        venue: event.venue,
        startsAt: toInputValue(event.startsAt),
        endsAt: toInputValue(event.endsAt),
        status: event.status,
      });
    });
  }, [isEditing, eventId, token]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;

    const request: EventRequest = {
      name: form.name,
      venue: form.venue,
      startsAt: toIso(form.startsAt),
      endsAt: toIso(form.endsAt),
      salesStart: null,
      salesEnd: null,
      status: form.status,
    };

    try {
      if (isEditing) {
        await apiRequest(`/api/v1/events/${eventId}`, { method: "PUT", body: request, token });
      } else {
        const created = await apiRequest<EventResponse>("/api/v1/events", {
          method: "POST",
          body: request,
          token,
        });
        navigate(`/organizer/events/${created.id}`, { replace: true });
        return;
      }
    } catch {
      setError("Couldn't save the event. Check the form and try again.");
    }
  }

  return (
    <section className="max-w-xl">
      <h1 className="text-2xl font-semibold tracking-tight">
        {isEditing ? "Edit event" : "New event"}
      </h1>

      {error && <p className="mt-4 text-red-600">{error}</p>}

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div>
          <label className="block text-sm font-medium">Name</label>
          <input
            required
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </div>
        <div>
          <label className="block text-sm font-medium">Venue</label>
          <input
            required
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
            value={form.venue}
            onChange={(e) => setForm({ ...form, venue: e.target.value })}
          />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium">Starts</label>
            <input
              required
              type="datetime-local"
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              value={form.startsAt}
              onChange={(e) => setForm({ ...form, startsAt: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium">Ends</label>
            <input
              required
              type="datetime-local"
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              value={form.endsAt}
              onChange={(e) => setForm({ ...form, endsAt: e.target.value })}
            />
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium">Status</label>
          <select
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
            value={form.status}
            onChange={(e) => setForm({ ...form, status: e.target.value as EventStatus })}
          >
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </div>
        <button
          type="submit"
          className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white"
        >
          Save event
        </button>
      </form>

      {isEditing && eventId && <EventTicketTypes eventId={eventId} />}
    </section>
  );
}
