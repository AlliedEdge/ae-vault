import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { tokenService } from '../services/tokenService';
import { useTokenRefresh } from '../hooks/useTokenRefresh';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: any;
  error: string | null;
  checkAuth: () => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const navigate = useNavigate();
  const [sessionCheckInterval, setSessionCheckInterval] = useState<ReturnType<typeof setInterval> | null>(null);
  
  // Initialize token refresh hook for automatic session restoration
  // This must happen BEFORE we check auth status
  useTokenRefresh();
  
  const {
    user,
    isAuthenticated,
    isLoading,
    error,
    isInitialized,
    checkAuth,
    logout: storeLogout,
    clearError,
  } = useAuthStore();

  /**
   * Initialize auth on mount
   */
  useEffect(() => {
    if (!isInitialized) {
      checkAuth();
    }
  }, [isInitialized, checkAuth]);

  /**
   * Setup session expiration check
   * DISABLED FOR DEVELOPMENT - Re-enable in production
   */
  useEffect(() => {
    if (!isAuthenticated) {
      // Clear interval if user is not authenticated
      if (sessionCheckInterval) {
        clearInterval(sessionCheckInterval);
        setSessionCheckInterval(null);
      }
      return;
    }

    // DISABLED FOR DEVELOPMENT
    // Check session every minute
    // const interval = setInterval(() => {
    //   if (tokenService.isTokenExpired()) {
    //     handleSessionExpired();
    //   }
    // }, 60 * 1000); // Check every 60 seconds

    // setSessionCheckInterval(interval);

    return () => {
      if (sessionCheckInterval) {
        clearInterval(sessionCheckInterval);
      }
    };
  }, [isAuthenticated]);

  /**
   * Setup auto-logout timer based on token expiry
   * DISABLED FOR DEVELOPMENT - Re-enable in production
   */
  useEffect(() => {
    if (!isAuthenticated) return;

    // DISABLED FOR DEVELOPMENT
    // const timeUntilExpiry = tokenService.getTimeUntilExpiry();
    
    // // Only set timeout if there's significant time remaining (more than 1 minute)
    // if (timeUntilExpiry > 60 * 1000) {
    //   // Set timeout to auto-logout when token expires
    //   const timeout = setTimeout(() => {
    //     handleSessionExpired();
    //   }, timeUntilExpiry);

    //   return () => {
    //     clearTimeout(timeout);
    //   };
    // }
  }, [isAuthenticated, user]);

  /**
   * Handle session expiration
   */
  const handleSessionExpired = useCallback(async () => {
    console.log('Session expired, logging out...');
    
    // Clear tokens
    tokenService.clearTokens();
    
    // Logout from store
    await storeLogout();
    
    // Redirect to session expired page
    navigate('/session-expired', { 
      replace: true,
      state: { from: window.location.pathname }
    });
  }, [storeLogout, navigate]);

  /**
   * Logout wrapper with navigation
   */
  const logout = useCallback(async () => {
    await storeLogout();
    navigate('/login', { replace: true });
  }, [storeLogout, navigate]);

  /**
   * Listen for token refresh failures (from axios interceptor)
   */
  useEffect(() => {
    const handleTokenRefreshFailed = (event: Event) => {
      const customEvent = event as CustomEvent;
      const detail = customEvent.detail;
      
      console.log('[AuthContext] Token refresh failed, logging out...', detail);
      
      // Clear tokens
      tokenService.clearTokens();
      
      // Logout from store
      storeLogout();
      
      // Redirect to login with appropriate message
      if (detail?.reason === 'no_refresh_token') {
        navigate('/login', { 
          replace: true,
          state: { 
            message: 'Your session has expired. Please login again.',
            from: window.location.pathname 
          }
        });
      } else {
        navigate('/login', { 
          replace: true,
          state: { 
            message: detail?.error || 'Authentication failed. Please login again.',
            from: window.location.pathname 
          }
        });
      }
    };

    window.addEventListener('auth:token-refresh-failed', handleTokenRefreshFailed);

    return () => {
      window.removeEventListener('auth:token-refresh-failed', handleTokenRefreshFailed);
    };
  }, [storeLogout, navigate]);

  /**
   * Listen for storage events (logout in another tab)
   */
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      // If refresh token was cleared in another tab, logout here too
      if (
        e.key === 'ziboto_refresh_token' &&
        e.newValue === null &&
        isAuthenticated
      ) {
        console.log('[AuthContext] Logged out in another tab');
        storeLogout();
        navigate('/login', { replace: true });
      }
    };

    window.addEventListener('storage', handleStorageChange);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
    };
  }, [isAuthenticated, storeLogout, navigate]);

  const value: AuthContextType = {
    isAuthenticated,
    isLoading,
    user,
    error,
    checkAuth,
    logout,
    clearError,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

/**
 * Hook to use auth context
 */
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  
  return context;
};
