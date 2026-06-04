import { Outlet } from "react-router-dom";
import { TopNav } from "./top-nav";

export function AppLayout() {
  return (
    <div className="min-h-screen bg-white text-slate-900">
      <TopNav />
      <main className="mx-auto max-w-5xl px-6 py-10">
        <Outlet />
      </main>
    </div>
  );
}
