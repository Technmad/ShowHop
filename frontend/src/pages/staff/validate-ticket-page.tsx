import { useState } from "react";
import { useAuth } from "react-oidc-context";
import { Scanner } from "@yudiel/react-qr-scanner";
import { apiRequest } from "../../lib/api";
import type { TicketValidationMethod, TicketValidationRequest, TicketValidationResponse } from "../../domain/types";

const STATUS_STYLES: Record<string, string> = {
  VALID: "bg-green-50 text-green-800 border-green-200",
  INVALID: "bg-red-50 text-red-800 border-red-200",
  EXPIRED: "bg-amber-50 text-amber-800 border-amber-200",
};

type Mode = "scan" | "manual";

export function ValidateTicketPage() {
  const auth = useAuth();
  const [mode, setMode] = useState<Mode>("scan");
  const [manualTicketId, setManualTicketId] = useState("");
  const [result, setResult] = useState<TicketValidationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [scannerPaused, setScannerPaused] = useState(false);

  async function validate(ticketId: string, method: TicketValidationMethod) {
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
    } catch {
      setError("Couldn't find that ticket. Check the id and try again.");
    }
  }

  async function handleManualSubmit(e: React.FormEvent) {
    e.preventDefault();
    await validate(manualTicketId, "MANUAL");
    setManualTicketId("");
  }

  async function handleScan(codes: { rawValue: string }[]) {
    const ticketId = codes[0]?.rawValue;
    if (!ticketId || scannerPaused) return;
    setScannerPaused(true); // freeze on the decoded frame until staff scans the next ticket
    await validate(ticketId, "QR_SCAN");
  }

  return (
    <section className="max-w-md">
      <h1 className="text-2xl font-semibold tracking-tight">Validate a ticket</h1>
      <p className="mt-1 text-sm text-slate-600">
        Scan a ticket's QR code with the camera, or enter its id manually.
      </p>

      <div className="mt-6 flex gap-4 border-b border-slate-200 text-sm">
        <button
          className={`border-b-2 px-1 pb-2 ${mode === "scan" ? "border-slate-900 font-medium" : "border-transparent text-slate-500"}`}
          onClick={() => setMode("scan")}
        >
          Scan
        </button>
        <button
          className={`border-b-2 px-1 pb-2 ${mode === "manual" ? "border-slate-900 font-medium" : "border-transparent text-slate-500"}`}
          onClick={() => setMode("manual")}
        >
          Manual entry
        </button>
      </div>

      {mode === "scan" && (
        <div className="mt-6">
          <div className="overflow-hidden rounded border border-slate-200">
            <Scanner onScan={handleScan} paused={scannerPaused} />
          </div>
          {scannerPaused && (
            <button
              onClick={() => {
                setScannerPaused(false);
                setResult(null);
                setError(null);
              }}
              className="mt-4 w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white"
            >
              Scan next ticket
            </button>
          )}
        </div>
      )}

      {mode === "manual" && (
        <form onSubmit={handleManualSubmit} className="mt-6 space-y-4">
          <div>
            <label className="block text-sm font-medium">Ticket id</label>
            <input
              required
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              value={manualTicketId}
              onChange={(e) => setManualTicketId(e.target.value)}
              placeholder="e.g. 3fa85f64-5717-4562-b3fc-2c963f66afa6"
            />
          </div>
          <button
            type="submit"
            className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white"
          >
            Validate
          </button>
        </form>
      )}

      {error && <p className="mt-6 text-red-600">{error}</p>}

      {result && (
        <div className={`mt-6 rounded border px-4 py-3 text-lg font-semibold ${STATUS_STYLES[result.status]}`}>
          {result.status}
        </div>
      )}
    </section>
  );
}
