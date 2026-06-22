import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { apiRequest } from "../../lib/api";
import type { TicketValidationMethod, TicketValidationRequest, TicketValidationResponse } from "../../domain/types";

const STATUS_STYLES: Record<string, string> = {
  VALID: "bg-green-50 text-green-800 border-green-200",
  INVALID: "bg-red-50 text-red-800 border-red-200",
  EXPIRED: "bg-amber-50 text-amber-800 border-amber-200",
};

export function ValidateTicketPage() {
  const auth = useAuth();
  const [ticketId, setTicketId] = useState("");
  const [method, setMethod] = useState<TicketValidationMethod>("MANUAL");
  const [result, setResult] = useState<TicketValidationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setResult(null);

    const request: TicketValidationRequest = { ticketId, method };
    try {
      const response = await apiRequest<TicketValidationResponse>("/api/v1/ticket-validations", {
        method: "POST",
        body: request,
        token: auth.user?.access_token,
      });
      setResult(response);
      setTicketId("");
    } catch {
      setError("Couldn't find that ticket. Check the id and try again.");
    }
  }

  return (
    <section className="max-w-md">
      <h1 className="text-2xl font-semibold tracking-tight">Validate a ticket</h1>
      <p className="mt-1 text-sm text-slate-600">
        Enter the ticket id from a QR scan or manually, then confirm entry.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div>
          <label className="block text-sm font-medium">Ticket id</label>
          <input
            required
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
            value={ticketId}
            onChange={(e) => setTicketId(e.target.value)}
            placeholder="e.g. 3fa85f64-5717-4562-b3fc-2c963f66afa6"
          />
        </div>
        <div className="flex gap-4 text-sm">
          <label className="flex items-center gap-2">
            <input
              type="radio"
              checked={method === "QR_SCAN"}
              onChange={() => setMethod("QR_SCAN")}
            />
            QR scan
          </label>
          <label className="flex items-center gap-2">
            <input
              type="radio"
              checked={method === "MANUAL"}
              onChange={() => setMethod("MANUAL")}
            />
            Manual entry
          </label>
        </div>
        <button
          type="submit"
          className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white"
        >
          Validate
        </button>
      </form>

      {error && <p className="mt-6 text-red-600">{error}</p>}

      {result && (
        <div className={`mt-6 rounded border px-4 py-3 text-lg font-semibold ${STATUS_STYLES[result.status]}`}>
          {result.status}
        </div>
      )}
    </section>
  );
}
