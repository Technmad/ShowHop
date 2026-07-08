import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { apiRequest } from "../../lib/api";
import type {
  Page,
  WebhookEndpointPatchRequest,
  WebhookEndpointRequest,
  WebhookEndpointResponse,
} from "../../domain/types";
import { WEBHOOK_EVENT_TYPE_OPTIONS } from "../../domain/types";

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: "bg-green-50 text-green-800 border-green-200",
  DISABLED: "bg-slate-100 text-slate-600 border-slate-200",
  CIRCUIT_OPEN: "bg-amber-50 text-amber-800 border-amber-200",
};

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "Active",
  DISABLED: "Disabled",
  CIRCUIT_OPEN: "Circuit open",
};

export function WebhookEndpointsPage() {
  const auth = useAuth();
  const token = auth.user?.access_token;

  const [endpoints, setEndpoints] = useState<WebhookEndpointResponse[] | null>(null);
  const [url, setUrl] = useState("");
  const [selectedTypes, setSelectedTypes] = useState<string[]>([]);
  const [revealedSecret, setRevealedSecret] = useState<{ endpointId: string; secret: string } | null>(null);
  const [error, setError] = useState<string | null>(null);

  function loadEndpoints() {
    if (!token) return;
    apiRequest<Page<WebhookEndpointResponse>>("/api/v1/webhook-endpoints", { token })
      .then((page) => setEndpoints(page.content))
      .catch(() => setError("Couldn't load your webhook endpoints."));
  }

  useEffect(loadEndpoints, [token]);

  function toggleType(value: string) {
    setSelectedTypes((current) =>
      current.includes(value) ? current.filter((t) => t !== value) : [...current, value],
    );
  }

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault();
    if (!token || selectedTypes.length === 0) return;

    const request: WebhookEndpointRequest = { url, subscribedEventTypes: selectedTypes };
    try {
      const created = await apiRequest<WebhookEndpointResponse>("/api/v1/webhook-endpoints", {
        method: "POST",
        body: request,
        token,
      });
      setRevealedSecret({ endpointId: created.id, secret: created.secret! });
      setUrl("");
      setSelectedTypes([]);
      setError(null);
      loadEndpoints();
    } catch {
      setError("Couldn't register that endpoint. Check the URL and try again.");
    }
  }

  async function patchEndpoint(endpointId: string, patch: WebhookEndpointPatchRequest) {
    if (!token) return;
    try {
      const updated = await apiRequest<WebhookEndpointResponse>(
        `/api/v1/webhook-endpoints/${endpointId}`,
        { method: "PATCH", body: patch, token },
      );
      if (updated.secret) {
        setRevealedSecret({ endpointId, secret: updated.secret });
      }
      loadEndpoints();
    } catch {
      setError("Couldn't update that endpoint.");
    }
  }

  return (
    <section>
      <h1 className="text-2xl font-semibold tracking-tight">Webhook endpoints</h1>
      <p className="mt-1 text-sm text-slate-600">
        Register a URL to receive signed, retried HTTP notifications when events happen on your
        account.
      </p>

      {error && <p className="mt-4 text-red-600">{error}</p>}

      {revealedSecret && (
        <div className="mt-6 rounded border border-amber-300 bg-amber-50 p-4 text-sm">
          <p className="font-medium text-amber-900">
            Signing secret -- copy it now, it won't be shown again:
          </p>
          <code className="mt-2 block break-all rounded bg-white px-3 py-2 text-amber-900">
            {revealedSecret.secret}
          </code>
          <button
            className="mt-3 text-xs underline"
            onClick={() => setRevealedSecret(null)}
          >
            Dismiss
          </button>
        </div>
      )}

      <form onSubmit={handleRegister} className="mt-6 max-w-lg space-y-4 rounded border border-slate-200 p-4">
        <h2 className="text-sm font-medium">Register a new endpoint</h2>
        <div>
          <label className="block text-sm font-medium">URL</label>
          <input
            required
            type="url"
            placeholder="https://your-service.example.com/webhooks/showhop"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
          />
        </div>
        <div>
          <label className="block text-sm font-medium">Subscribed events</label>
          <div className="mt-2 space-y-2">
            {WEBHOOK_EVENT_TYPE_OPTIONS.map((option) => (
              <label key={option.value} className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={selectedTypes.includes(option.value)}
                  onChange={() => toggleType(option.value)}
                />
                {option.label}
              </label>
            ))}
          </div>
        </div>
        <button
          type="submit"
          disabled={selectedTypes.length === 0}
          className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          Register endpoint
        </button>
      </form>

      <ul className="mt-8 divide-y divide-slate-200">
        {endpoints?.map((endpoint) => (
          <li key={endpoint.id} className="py-4">
            <div className="flex items-center justify-between gap-4">
              <div className="min-w-0">
                <p className="truncate font-medium">{endpoint.url}</p>
                <p className="text-sm text-slate-600">
                  {endpoint.subscribedEventTypes.join(", ")}
                </p>
              </div>
              <span
                className={`shrink-0 rounded border px-2 py-1 text-xs font-medium ${STATUS_STYLES[endpoint.status]}`}
              >
                {STATUS_LABELS[endpoint.status]}
              </span>
            </div>

            {endpoint.consecutiveFailures > 0 && (
              <p className="mt-1 text-xs text-slate-500">
                {endpoint.consecutiveFailures} consecutive failure
                {endpoint.consecutiveFailures === 1 ? "" : "s"}
              </p>
            )}

            <div className="mt-3 flex flex-wrap gap-4 text-sm">
              <Link to={`/organizer/webhooks/${endpoint.id}/deliveries`} className="underline">
                View deliveries
              </Link>
              <button
                className="underline"
                onClick={() => patchEndpoint(endpoint.id, { enabled: endpoint.status !== "ACTIVE" })}
              >
                {endpoint.status === "ACTIVE" ? "Disable" : "Enable"}
              </button>
              <button
                className="underline"
                onClick={() => patchEndpoint(endpoint.id, { rotateSecret: true })}
              >
                Rotate secret
              </button>
            </div>
          </li>
        ))}
        {endpoints && endpoints.length === 0 && (
          <p className="py-4 text-slate-600">No webhook endpoints registered yet.</p>
        )}
      </ul>
    </section>
  );
}
