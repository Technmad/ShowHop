import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { apiRequest } from "../../lib/api";
import type { Page, WebhookDeliveryResponse } from "../../domain/types";

const STATE_STYLES: Record<string, string> = {
  PENDING: "bg-slate-100 text-slate-600 border-slate-200",
  IN_FLIGHT: "bg-blue-50 text-blue-800 border-blue-200",
  RETRYING: "bg-amber-50 text-amber-800 border-amber-200",
  SUCCEEDED: "bg-green-50 text-green-800 border-green-200",
  DEAD_LETTER: "bg-red-50 text-red-800 border-red-200",
};

export function WebhookDeliveriesPage() {
  const { endpointId } = useParams<{ endpointId: string }>();
  const auth = useAuth();
  const token = auth.user?.access_token;

  const [deliveries, setDeliveries] = useState<WebhookDeliveryResponse[] | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function loadDeliveries() {
    if (!token || !endpointId) return;
    apiRequest<Page<WebhookDeliveryResponse>>(
      `/api/v1/webhook-endpoints/${endpointId}/deliveries`, { token },
    )
      .then((page) => setDeliveries(page.content))
      .catch(() => setError("Couldn't load deliveries for that endpoint."));
  }

  useEffect(loadDeliveries, [token, endpointId]);

  async function replay(deliveryId: string) {
    if (!token) return;
    try {
      await apiRequest(`/api/v1/webhook-deliveries/${deliveryId}/replay`, {
        method: "POST",
        token,
      });
      loadDeliveries();
    } catch {
      setError("Couldn't replay that delivery.");
    }
  }

  return (
    <section>
      <Link to="/organizer/webhooks" className="text-sm underline">
        &larr; Back to endpoints
      </Link>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">Delivery log</h1>

      {error && <p className="mt-4 text-red-600">{error}</p>}

      <ul className="mt-6 divide-y divide-slate-200">
        {deliveries?.map((delivery) => (
          <li key={delivery.id} className="py-4">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="font-medium">
                  {delivery.eventType}
                  {delivery.probe && (
                    <span className="ml-2 text-xs font-normal text-slate-500">(probe)</span>
                  )}
                </p>
                <p className="text-sm text-slate-600">
                  Attempt {delivery.attempt}/{delivery.maxAttempts}
                  {delivery.lastResponseCode !== null && ` · HTTP ${delivery.lastResponseCode}`}
                </p>
              </div>
              <span
                className={`shrink-0 rounded border px-2 py-1 text-xs font-medium ${STATE_STYLES[delivery.state]}`}
              >
                {delivery.state}
              </span>
            </div>

            <div className="mt-2 flex flex-wrap gap-4 text-sm">
              {delivery.lastError && (
                <button
                  className="underline"
                  onClick={() => setExpandedId(expandedId === delivery.id ? null : delivery.id)}
                >
                  {expandedId === delivery.id ? "Hide details" : "Show details"}
                </button>
              )}
              {(delivery.state === "DEAD_LETTER" || delivery.state === "SUCCEEDED") && (
                <button className="underline" onClick={() => replay(delivery.id)}>
                  Replay
                </button>
              )}
            </div>

            {expandedId === delivery.id && delivery.lastError && (
              <pre className="mt-2 overflow-x-auto rounded bg-slate-50 p-3 text-xs text-slate-700">
                {delivery.lastError}
              </pre>
            )}
          </li>
        ))}
        {deliveries && deliveries.length === 0 && (
          <p className="py-4 text-slate-600">No deliveries yet for this endpoint.</p>
        )}
      </ul>
    </section>
  );
}
