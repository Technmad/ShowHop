import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { ApiError, apiRequest } from "../../lib/api";
import type {
  PublishedEventResponse,
  ReservationInitiationResponse,
  ReservationStatusResponse,
  TicketTypeResponse,
} from "../../domain/types";

interface RazorpayCheckoutOptions {
  key: string;
  amount: number;
  currency: string;
  order_id: string;
  name: string;
  description?: string;
  handler: () => void;
  modal?: { ondismiss?: () => void };
}

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayCheckoutOptions) => { open(): void };
  }
}

const CHECKOUT_SCRIPT_SRC = "https://checkout.razorpay.com/v1/checkout.js";
const POLL_INTERVAL_MS = 2000;
// A generous upper bound on how long this page waits -- the reservation's
// own server-side TTL (PRD 4.2) is the real limit; this just stops the
// browser polling forever if something is stuck.
const POLL_TIMEOUT_MS = 5 * 60 * 1000;

function loadRazorpayCheckoutScript(): Promise<void> {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${CHECKOUT_SCRIPT_SRC}"]`);
    if (existing) {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () => reject(new Error("Couldn't load Razorpay Checkout.")));
      return;
    }
    const script = document.createElement("script");
    script.src = CHECKOUT_SCRIPT_SRC;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Couldn't load Razorpay Checkout."));
    document.body.appendChild(script);
  });
}

type CheckoutStage = "form" | "processing" | "failed";

/**
 * Reserve -> Razorpay Checkout -> poll -> confirmed, per the purchase saga
 * in docs/PRD.md &sect;4.2. The Checkout `handler` callback below is
 * deliberately never what fulfills the ticket -- it can fire on a spoofed
 * client, or not fire at all if the tab closes mid-UPI-approval. Only the
 * backend's inbound webhook creates a Ticket; this page just polls
 * GET /api/v1/reservations/{id} until that's happened.
 */
export function PurchasePage() {
  const { eventId, ticketTypeId } = useParams<{ eventId: string; ticketTypeId: string }>();
  const auth = useAuth();
  const navigate = useNavigate();

  const [event, setEvent] = useState<PublishedEventResponse | null>(null);
  const [ticketType, setTicketType] = useState<TicketTypeResponse | null>(null);
  const [stage, setStage] = useState<CheckoutStage>("form");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const pollHandle = useRef<number | null>(null);

  useEffect(() => {
    if (!eventId || !ticketTypeId) return;
    apiRequest<PublishedEventResponse>(`/api/v1/published-events/${eventId}`).then(setEvent);
    apiRequest<{ content: TicketTypeResponse[] }>(
      `/api/v1/published-events/${eventId}/ticket-types`,
    ).then((page) => setTicketType(page.content.find((tt) => tt.id === ticketTypeId) ?? null));
  }, [eventId, ticketTypeId]);

  useEffect(() => stopPolling, []);

  function stopPolling() {
    if (pollHandle.current !== null) {
      window.clearInterval(pollHandle.current);
      pollHandle.current = null;
    }
  }

  function pollReservationStatus(reservationId: string, token: string) {
    const startedAt = Date.now();
    pollHandle.current = window.setInterval(async () => {
      if (Date.now() - startedAt > POLL_TIMEOUT_MS) {
        stopPolling();
        setStage("failed");
        setError("This is taking longer than expected -- check \"Your tickets\" in a moment.");
        return;
      }

      try {
        const status = await apiRequest<ReservationStatusResponse>(
          `/api/v1/reservations/${reservationId}`, { token },
        );
        if (status.state === "CONFIRMED") {
          stopPolling();
          navigate("/tickets");
        } else if (status.state === "EXPIRED" || status.state === "FAILED" || status.state === "CANCELLED") {
          stopPolling();
          setStage("failed");
          setError("The payment didn't go through. Please try again.");
        }
        // HELD -- still processing, keep polling.
      } catch {
        // A transient network hiccup; the next tick retries.
      }
    }, POLL_INTERVAL_MS);
  }

  async function handlePay() {
    if (!eventId || !ticketTypeId || !auth.user?.access_token) return;
    const token = auth.user.access_token;

    setSubmitting(true);
    setError(null);
    try {
      const reservation = await apiRequest<ReservationInitiationResponse>(
        `/api/v1/published-events/${eventId}/ticket-types/${ticketTypeId}/reservations`,
        {
          method: "POST",
          token,
          body: { quantity: 1 },
          headers: { "Idempotency-Key": crypto.randomUUID() },
        },
      );

      await loadRazorpayCheckoutScript();
      if (!window.Razorpay) {
        throw new Error("Razorpay Checkout unavailable");
      }

      setStage("processing");
      new window.Razorpay({
        key: reservation.razorpayKeyId,
        amount: reservation.amount,
        currency: "INR",
        order_id: reservation.razorpayOrderId,
        name: "ShowHop",
        description: event?.name,
        handler: () => {
          // Optimistic-UI only, per PRD 4.2 -- fulfillment happens only
          // when the webhook confirms it; polling below is what notices.
        },
        modal: {
          ondismiss: () => {
            // Buyer closed the modal without an apparent success -- keep
            // polling anyway; a UPI payment can still land after this.
          },
        },
      }).open();

      pollReservationStatus(reservation.id, token);
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409
        ? "That ticket type just sold out."
        : "Couldn't start checkout. Please try again.");
      setSubmitting(false);
    }
  }

  if (!event || !ticketType) {
    return <p className="text-slate-600">Loading...</p>;
  }

  return (
    <section className="max-w-sm">
      <h1 className="text-2xl font-semibold tracking-tight">Confirm your order</h1>

      <div className="mt-4 rounded border border-slate-200 p-4">
        <p className="font-medium">{event.name}</p>
        <p className="text-sm text-slate-600">{ticketType.name}</p>
        <p className="mt-2 text-lg font-semibold">&#8377;{ticketType.price}</p>
      </div>

      {stage === "processing" && (
        <p className="mt-6 text-sm text-slate-600">
          Processing your payment... this page moves on automatically once it's confirmed.
        </p>
      )}

      {error && <p className="mt-4 text-red-600">{error}</p>}

      {stage !== "processing" && (
        <button
          onClick={handlePay}
          disabled={submitting}
          className="mt-6 w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {submitting ? "Starting checkout..." : `Pay ₹${ticketType.price}`}
        </button>
      )}
    </section>
  );
}
