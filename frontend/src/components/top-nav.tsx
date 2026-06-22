import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";

export function TopNav() {
  const auth = useAuth();

  return (
    <header className="border-b border-slate-200">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <Link to="/" className="text-lg font-semibold tracking-tight">
          ShowHop
        </Link>
        <div className="flex items-center gap-6 text-sm text-slate-600">
          <Link to="/">Browse events</Link>
          {auth.isAuthenticated && <Link to="/tickets">My tickets</Link>}
          <Link to="/organizer/events">Organize</Link>
          {auth.isAuthenticated ? (
            <button onClick={() => auth.signoutRedirect()} className="underline">
              Sign out
            </button>
          ) : (
            <button onClick={() => auth.signinRedirect()} className="underline">
              Sign in
            </button>
          )}
        </div>
      </nav>
    </header>
  );
}
