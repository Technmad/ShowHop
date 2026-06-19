import type { ReactNode } from "react";
import { useAuth } from "react-oidc-context";
import { useHasRole } from "../hooks/use-roles";

interface ProtectedRouteProps {
  requireRole?: string;
  children: ReactNode;
}

export function ProtectedRoute({ requireRole, children }: ProtectedRouteProps) {
  const auth = useAuth();
  const hasRole = useHasRole(requireRole ?? "");

  if (auth.isLoading) {
    return <p className="text-slate-600">Loading...</p>;
  }

  if (!auth.isAuthenticated) {
    return (
      <div className="space-y-4">
        <p className="text-slate-600">You need to sign in to view this page.</p>
        <button
          className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white"
          onClick={() => auth.signinRedirect()}
        >
          Sign in
        </button>
      </div>
    );
  }

  if (requireRole && !hasRole) {
    return <p className="text-slate-600">You don't have access to this page.</p>;
  }

  return <>{children}</>;
}
