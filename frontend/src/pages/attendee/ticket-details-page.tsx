import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { apiRequest, apiRequestBlobUrl } from "../../lib/api";
import type { TicketResponse } from "../../domain/types";

export function TicketDetailsPage() {
  const { ticketId } = useParams<{ ticketId: string }>();
  const auth = useAuth();
  const token = auth.user?.access_token;

  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [qrCodeUrl, setQrCodeUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!ticketId || !token) return;
    apiRequest<TicketResponse>(`/api/v1/tickets/${ticketId}`, { token }).then(setTicket);

    let objectUrl: string | null = null;
    apiRequestBlobUrl(`/api/v1/tickets/${ticketId}/qr-codes`, token).then((url) => {
      objectUrl = url;
      setQrCodeUrl(url);
    });
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [ticketId, token]);

  if (!ticket) {
    return <p className="text-slate-600">Loading...</p>;
  }

  return (
    <section className="max-w-sm">
      <h1 className="text-2xl font-semibold tracking-tight">Your ticket</h1>
      <p className="mt-1 text-sm text-slate-600">Status: {ticket.status}</p>

      {qrCodeUrl && (
        <img
          src={qrCodeUrl}
          alt="Ticket QR code"
          className="mt-6 w-64 rounded border border-slate-200"
        />
      )}
      <p className="mt-4 text-sm text-slate-600">
        Show this code at the door -- staff will scan or enter it manually.
      </p>
    </section>
  );
}
