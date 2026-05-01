import { Navigate, Outlet, createBrowserRouter } from "react-router-dom";
import { LoginPage } from "@/pages/Login";
import { RegisterPage } from "@/pages/Register";
import { LandingPage } from "@/pages/Landing";
import { useAuth } from "@/lib/auth";

function ProtectedRoute() {
  const { isAuthenticated, isInitializing } = useAuth();
  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted-foreground">
        Loading…
      </div>
    );
  }
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}

function PublicOnlyRoute() {
  const { isAuthenticated, isInitializing } = useAuth();
  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted-foreground">
        Loading…
      </div>
    );
  }
  return isAuthenticated ? <Navigate to="/" replace /> : <Outlet />;
}

export const router = createBrowserRouter([
  {
    element: <ProtectedRoute />,
    children: [{ path: "/", element: <LandingPage /> }],
  },
  {
    element: <PublicOnlyRoute />,
    children: [
      { path: "/login", element: <LoginPage /> },
      { path: "/register", element: <RegisterPage /> },
    ],
  },
  { path: "*", element: <Navigate to="/" replace /> },
]);
