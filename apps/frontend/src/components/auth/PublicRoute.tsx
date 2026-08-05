import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Loader2 } from 'lucide-react';

interface PublicRouteProps {
  children: React.ReactNode;
  redirectTo?: string;
}

/**
 * Public Route Component
 * Redirects authenticated users away from auth pages (login, register, etc.)
 */
export const PublicRoute: React.FC<PublicRouteProps> = ({ 
  children, 
  redirectTo = '/' 
}) => {
  const { isAuthenticated, isLoading } = useAuth();

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div className="min-h-screen w-full flex items-center justify-center bg-dark-950">
        <div className="text-center space-y-4">
          <Loader2 className="w-12 h-12 text-primary-500 animate-spin mx-auto" />
          <p className="text-dark-300">Loading...</p>
        </div>
      </div>
    );
  }

  // Redirect to home if already authenticated
  if (isAuthenticated) {
    return <Navigate to={redirectTo} replace />;
  }

  // Render children if not authenticated
  return <>{children}</>;
};
