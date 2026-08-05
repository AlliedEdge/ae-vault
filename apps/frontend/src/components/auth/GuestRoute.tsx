import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Loader2, UserCheck } from 'lucide-react';

interface GuestRouteProps {
  children: React.ReactNode;
  redirectTo?: string;
}

/**
 * Guest Route Component
 * 
 * Purpose:
 * - Protects auth pages (login, register) from authenticated users
 * - Redirects authenticated users to initialization page or specified path
 * - Shows loading state while authentication is being verified
 * - Prevents authenticated users from accessing guest-only pages
 * 
 * Usage:
 * ```tsx
 * <Route path="/login" element={
 *   <GuestRoute>
 *     <Login />
 *   </GuestRoute>
 * } />
 * ```
 */
export const GuestRoute: React.FC<GuestRouteProps> = ({ 
  children, 
  redirectTo = '/initializing' 
}) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div className="min-h-screen w-full flex items-center justify-center bg-dark-950">
        <div className="text-center space-y-4">
          <div className="relative">
            <UserCheck className="w-16 h-16 text-primary-500/20 mx-auto" />
            <Loader2 className="w-8 h-8 text-primary-500 animate-spin absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" />
          </div>
          <div className="space-y-1">
            <p className="text-dark-200 font-medium">Checking authentication...</p>
            <p className="text-dark-400 text-sm">Please wait</p>
          </div>
        </div>
      </div>
    );
  }

  // Redirect to home (or specified path) if already authenticated
  if (isAuthenticated) {
    // Check if there's a redirect path in location state (from login)
    const from = (location.state as any)?.from;
    const destination = from || redirectTo;
    
    return <Navigate to={destination} replace />;
  }

  // Render children if not authenticated (guest)
  return <>{children}</>;
};
