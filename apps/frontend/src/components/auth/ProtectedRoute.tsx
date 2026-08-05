import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Loader2, Shield } from 'lucide-react';

interface ProtectedRouteProps {
  children: React.ReactNode;
  fallbackPath?: string;
}

/**
 * Protected Route Component
 * 
 * Purpose:
 * - Protects routes that require authentication
 * - Redirects unauthenticated users to login page
 * - Preserves the intended destination for post-login redirect
 * - Shows loading state while authentication is being verified
 * 
 * Usage:
 * ```tsx
 * <Route path="/dashboard" element={
 *   <ProtectedRoute>
 *     <Dashboard />
 *   </ProtectedRoute>
 * } />
 * ```
 */
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children,
  fallbackPath = '/login'
}) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div className="min-h-screen w-full flex items-center justify-center bg-dark-950">
        <div className="text-center space-y-4">
          <div className="relative">
            <Shield className="w-16 h-16 text-primary-500/20 mx-auto" />
            <Loader2 className="w-8 h-8 text-primary-500 animate-spin absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" />
          </div>
          <div className="space-y-1">
            <p className="text-dark-200 font-medium">Verifying authentication...</p>
            <p className="text-dark-400 text-sm">Please wait</p>
          </div>
        </div>
      </div>
    );
  }

  // Redirect to login if not authenticated
  // Preserve the intended destination in location state
  if (!isAuthenticated) {
    return (
      <Navigate 
        to={fallbackPath} 
        state={{ 
          from: location.pathname,
          message: 'Please login to access this page.'
        }} 
        replace 
      />
    );
  }

  // Render children if authenticated
  return <>{children}</>;
};
