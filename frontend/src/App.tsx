import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/app-layout";
import { ProtectedRoute } from "./components/protected-route";
import { AuthProvider } from "./lib/auth";
import { HomePage } from "./routes/home-page";
import { CallbackPage } from "./routes/callback-page";
import { EventFormPage } from "./pages/organizer/event-form-page";
import { OrganizerEventsPage } from "./pages/organizer/organizer-events-page";
import { OrganizerLandingPage } from "./pages/organizer/organizer-landing-page";
import { EventDetailsPage } from "./pages/attendee/event-details-page";
import { PurchasePage } from "./pages/attendee/purchase-page";
import { MyTicketsPage } from "./pages/attendee/my-tickets-page";
import { TicketDetailsPage } from "./pages/attendee/ticket-details-page";
import { ValidateTicketPage } from "./pages/staff/validate-ticket-page";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="callback" element={<CallbackPage />} />
            <Route path="events/:eventId" element={<EventDetailsPage />} />
            <Route
              path="events/:eventId/ticket-types/:ticketTypeId/purchase"
              element={
                <ProtectedRoute>
                  <PurchasePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="tickets"
              element={
                <ProtectedRoute>
                  <MyTicketsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="tickets/:ticketId"
              element={
                <ProtectedRoute>
                  <TicketDetailsPage />
                </ProtectedRoute>
              }
            />
            <Route path="organizer" element={<OrganizerLandingPage />} />
            <Route
              path="organizer/events"
              element={
                <ProtectedRoute requireRole="ORGANIZER">
                  <OrganizerEventsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="organizer/events/:eventId"
              element={
                <ProtectedRoute requireRole="ORGANIZER">
                  <EventFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="staff/validate"
              element={
                <ProtectedRoute requireRole="STAFF">
                  <ValidateTicketPage />
                </ProtectedRoute>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
