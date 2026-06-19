import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/app-layout";
import { ProtectedRoute } from "./components/protected-route";
import { AuthProvider } from "./lib/auth";
import { HomePage } from "./routes/home-page";
import { CallbackPage } from "./routes/callback-page";
import { EventFormPage } from "./pages/organizer/event-form-page";
import { OrganizerEventsPage } from "./pages/organizer/organizer-events-page";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="callback" element={<CallbackPage />} />
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
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
