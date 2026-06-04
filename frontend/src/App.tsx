import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/app-layout";
import { HomePage } from "./routes/home-page";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
