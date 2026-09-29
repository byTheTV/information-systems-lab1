import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth";
import { Layout } from "./components/Layout";
import { LabWorkCreatePage } from "./pages/LabWorkCreatePage";
import { LabWorkListPage } from "./pages/LabWorkListPage";
import { LabWorkViewPage } from "./pages/LabWorkViewPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { SpecialPage } from "./pages/SpecialPage";

function RequireAuth() {
  const auth = useAuth();
  if (!auth.ready) return <p className="pad">Проверка сессии…</p>;
  if (!auth.token) return <Navigate to="/login" replace />;
  return <Layout />;
}

function AdminRoute() {
  const auth = useAuth();
  if (auth.role !== "ADMIN") return <Navigate to="/" replace />;
  return <LabWorkCreatePage />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/" element={<LabWorkListPage />} />
        <Route path="/labworks/new" element={<AdminRoute />} />
        <Route path="/labworks/:id" element={<LabWorkViewPage />} />
        <Route path="/special" element={<SpecialPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
