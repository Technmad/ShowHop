import { Link } from "react-router-dom";

export function TopNav() {
  return (
    <header className="border-b border-slate-200">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <Link to="/" className="text-lg font-semibold tracking-tight">
          ShowHop
        </Link>
        <div className="flex gap-6 text-sm text-slate-600">
          <Link to="/">Browse events</Link>
        </div>
      </nav>
    </header>
  );
}
