import { useEffect, useState } from "react";
import { apiRequest } from "../../lib/api";
import { useAuth } from "react-oidc-context";
import type { Page, TicketTypeRequest, TicketTypeResponse } from "../../domain/types";

const EMPTY_FORM = { name: "", description: "", price: "", totalAvailable: "" };

export function EventTicketTypes({ eventId }: { eventId: string }) {
  const auth = useAuth();
  const token = auth.user?.access_token;
  const [ticketTypes, setTicketTypes] = useState<TicketTypeResponse[]>([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!token) return;
    apiRequest<Page<TicketTypeResponse>>(`/api/v1/events/${eventId}/ticket-types`, { token })
      .then((page) => setTicketTypes(page.content))
      .catch(() => setError("Couldn't load ticket types."));
  }

  useEffect(load, [eventId, token]);

  async function addTicketType(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;
    const request: TicketTypeRequest = {
      name: form.name,
      description: form.description || null,
      price: form.price,
      totalAvailable: Number(form.totalAvailable),
    };
    try {
      await apiRequest(`/api/v1/events/${eventId}/ticket-types`, {
        method: "POST",
        body: request,
        token,
      });
      setForm(EMPTY_FORM);
      load();
    } catch {
      setError("Couldn't create the ticket type.");
    }
  }

  async function removeTicketType(ticketTypeId: string) {
    if (!token) return;
    await apiRequest(`/api/v1/events/${eventId}/ticket-types/${ticketTypeId}`, {
      method: "DELETE",
      token,
    });
    load();
  }

  return (
    <div className="mt-8 border-t border-slate-200 pt-6">
      <h2 className="text-lg font-semibold">Ticket types</h2>
      {error && <p className="mt-2 text-red-600">{error}</p>}

      <ul className="mt-4 divide-y divide-slate-200">
        {ticketTypes.map((tt) => (
          <li key={tt.id} className="flex items-center justify-between py-3">
            <div>
              <p className="font-medium">{tt.name}</p>
              <p className="text-sm text-slate-600">
                ${tt.price} · {tt.totalAvailable} available
              </p>
            </div>
            <button
              onClick={() => removeTicketType(tt.id)}
              className="text-sm text-red-600 underline"
            >
              Remove
            </button>
          </li>
        ))}
      </ul>

      <form onSubmit={addTicketType} className="mt-6 grid grid-cols-2 gap-3">
        <input
          required
          placeholder="Name (e.g. General Admission)"
          className="col-span-2 rounded border border-slate-300 px-3 py-2 text-sm"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
        />
        <input
          placeholder="Description (optional)"
          className="col-span-2 rounded border border-slate-300 px-3 py-2 text-sm"
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
        />
        <input
          required
          type="number"
          step="0.01"
          min="0"
          placeholder="Price"
          className="rounded border border-slate-300 px-3 py-2 text-sm"
          value={form.price}
          onChange={(e) => setForm({ ...form, price: e.target.value })}
        />
        <input
          required
          type="number"
          min="1"
          placeholder="Total available"
          className="rounded border border-slate-300 px-3 py-2 text-sm"
          value={form.totalAvailable}
          onChange={(e) => setForm({ ...form, totalAvailable: e.target.value })}
        />
        <button
          type="submit"
          className="col-span-2 rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white"
        >
          Add ticket type
        </button>
      </form>
    </div>
  );
}
