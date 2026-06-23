import { Link } from "react-router-dom";

export function OrganizerLandingPage() {
  return (
    <section className="text-center">
      <h1 className="text-3xl font-semibold tracking-tight">
        Create, manage, and sell tickets
      </h1>
      <p className="mx-auto mt-3 max-w-md text-slate-600">
        Set up ticket types with your own pricing and capacity, publish
        when you're ready, and watch tickets sell -- all from one place.
      </p>
      <div className="mt-8 flex justify-center gap-4">
        <Link
          to="/organizer/events/new"
          className="rounded bg-slate-900 px-5 py-2.5 text-sm font-medium text-white"
        >
          Create an event
        </Link>
        <Link
          to="/organizer/events"
          className="rounded border border-slate-300 px-5 py-2.5 text-sm font-medium"
        >
          View your events
        </Link>
      </div>
    </section>
  );
}
