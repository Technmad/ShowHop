import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { apiRequest } from "../../lib/api";
import type { Page, TicketResponse } from "../../domain/types";

export function MyTicketsPage() {
  const auth = useAuth();
  const [tickets, setTickets] = useState<TicketResponse[] | null>(null);

  useEffect(() => {
    if (!auth.user?.access_token) return;
    apiRequest<Page<TicketResponse>>("/api/v1/tickets", { token: auth.user.access_token }).then(
      (page) => setTickets(page.content),
    );
  }, [auth.user?.access_token]);

  return (
    <section>
      <h1 className="text-2xl font-semibold tracking-tight">Your tickets</h1>

      {tickets && tickets.length === 0 && (
        <p className="mt-6 text-slate-600">You haven't bought any tickets yet.</p>
      )}

      <ul className="mt-6 divide-y divide-slate-200">
        {tickets?.map((ticket) => (
          <li key={ticket.id} className="flex items-center justify-between py-4">
            <p className="text-sm text-slate-600">
              Ticket {ticket.id.slice(0, 8)} · {ticket.status}
            </p>
            <Link to={`/tickets/${ticket.id}`} className="text-sm underline">
              View QR code
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
