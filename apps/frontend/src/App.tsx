import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute, GuestRoute } from './components/auth';
import {
  Home,
  Login,
  Register,
  ForgotPassword,
  ResetPassword,
  EmailVerificationSuccess,
  SessionExpired,
  InitializingApp,
} from './pages';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* Initialization route - protected, shown after login/register */}
          <Route
            path="/initializing"
            element={
              <ProtectedRoute>
                <InitializingApp />
              </ProtectedRoute>
            }
          />

          {/* Protected route - Home (legacy, kept for compatibility) */}
          <Route
            path="/home"
            element={
              <ProtectedRoute>
                <Home />
              </ProtectedRoute>
            }
          />

          {/* Root redirects to initialization if authenticated, otherwise to login */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Navigate to="/initializing" replace />
              </ProtectedRoute>
            }
          />

          {/* Guest routes - redirect to initialization if authenticated */}
          <Route
            path="/login"
            element={
              <GuestRoute>
                <Login />
              </GuestRoute>
            }
          />
          <Route
            path="/register"
            element={
              <GuestRoute>
                <Register />
              </GuestRoute>
            }
          />

          {/* Auth utility routes - accessible to all */}
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/verify-email" element={<EmailVerificationSuccess />} />
          <Route path="/session-expired" element={<SessionExpired />} />

          {/* Catch all - redirect to root */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
