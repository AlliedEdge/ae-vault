/**
 * useTokenRefresh Hook
 * Handles automatic token refresh on page load and proactive refresh before expiry
 */

import { useEffect, useCallback, useRef } from 'react';
import { tokenService } from '../services/tokenService';
import { authService } from '../services/authService';
import { useAuthStore } from '../store/authStore';

/**
 * Custom hook to handle token refresh logic
 * 
 * Features:
 * 1. Restores session on page load if refresh token is available
 * 2. Proactively refreshes access token before expiry
 * 3. Handles refresh failures gracefully
 */
export const useTokenRefresh = () => {
  const { setUser, logout } = useAuthStore();
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isRefreshingRef = useRef(false);

  /**
   * Attempt to refresh the access token using refresh token
   */
  const refreshAccessToken = useCallback(async () => {
    // Prevent concurrent refresh attempts
    if (isRefreshingRef.current) {
      console.log('[useTokenRefresh] Refresh already in progress, skipping');
      return false;
    }

    const refreshToken = tokenService.getRefreshToken();
    
    if (!refreshToken) {
      console.log('[useTokenRefresh] No refresh token available');
      return false;
    }

    console.log('[useTokenRefresh] Starting token refresh with token:', refreshToken.substring(0, 30) + '...');

    try {
      isRefreshingRef.current = true;
      console.log('[useTokenRefresh] Refreshing access token...');
      
      const response = await authService.refreshToken(refreshToken);
      
      console.log('[useTokenRefresh] Refresh response received:', {
        hasAccessToken: !!response.accessToken,
        hasRefreshToken: !!response.refreshToken,
        accessTokenPreview: response.accessToken?.substring(0, 20) + '...',
        refreshTokenPreview: response.refreshToken?.substring(0, 20) + '...',
      });
      
      // Update tokens (only access token if backend doesn't rotate refresh token)
      if (response.refreshToken) {
        console.log('[useTokenRefresh] Storing NEW access token and NEW refresh token');
        tokenService.setTokens(response.accessToken, response.refreshToken);
      } else {
        console.log('[useTokenRefresh] Storing NEW access token, keeping OLD refresh token');
        tokenService.setAccessToken(response.accessToken);
      }
      
      // Fetch full user profile after token refresh to update auth state
      try {
        const user = await authService.getProfile();
        setUser(user);
        console.log('[useTokenRefresh] Token refresh successful, user profile loaded');
      } catch (profileError) {
        console.error('[useTokenRefresh] Failed to load user profile after token refresh:', profileError);
        // Token refresh was successful but profile fetch failed
        // We'll still return true since the token is valid
      }
      
      return true;
    } catch (error) {
      console.error('[useTokenRefresh] Token refresh failed:', error);
      
      // Clear tokens and logout on refresh failure
      tokenService.clearTokens();
      await logout();
      
      return false;
    } finally {
      isRefreshingRef.current = false;
    }
  }, [setUser, logout]);

  /**
   * Setup proactive token refresh timer
   * Refreshes token 2 minutes before expiry
   */
  const setupRefreshTimer = useCallback(() => {
    // Clear existing timer
    if (refreshTimerRef.current) {
      clearTimeout(refreshTimerRef.current);
    }

    const timeUntilExpiry = tokenService.getTimeUntilExpiry();
    
    if (timeUntilExpiry <= 0) {
      console.log('[useTokenRefresh] Token already expired');
      return;
    }

    // Refresh 2 minutes before expiry (or halfway through token lifetime, whichever is sooner)
    const refreshBuffer = Math.min(2 * 60 * 1000, timeUntilExpiry / 2);
    const refreshTime = timeUntilExpiry - refreshBuffer;

    console.log(`[useTokenRefresh] Scheduling token refresh in ${Math.round(refreshTime / 1000)}s`);

    refreshTimerRef.current = setTimeout(async () => {
      console.log('[useTokenRefresh] Proactive token refresh triggered');
      await refreshAccessToken();
      
      // Setup next refresh
      setupRefreshTimer();
    }, refreshTime);
  }, [refreshAccessToken]);

  /**
   * Restore session on mount if refresh token is available
   * but access token is missing (e.g., after page refresh)
   */
  useEffect(() => {
    const restoreSession = async () => {
      const hasAccessToken = !!tokenService.getAccessToken();
      const hasRefreshToken = tokenService.hasRefreshToken();

      console.log('[useTokenRefresh] Restore session check:', { hasAccessToken, hasRefreshToken });

      // If we have refresh token but no access token, restore session
      if (!hasAccessToken && hasRefreshToken) {
        console.log('[useTokenRefresh] Access token missing, restoring session...');
        const success = await refreshAccessToken();
        
        if (success) {
          // refreshAccessToken already loads the user profile
          setupRefreshTimer();
        }
      } else if (hasAccessToken && hasRefreshToken) {
        // If we have both tokens, just setup the refresh timer
        console.log('[useTokenRefresh] Both tokens present, setting up refresh timer');
        setupRefreshTimer();
      } else {
        console.log('[useTokenRefresh] No valid tokens, skipping session restoration');
      }
    };

    restoreSession();

    // Cleanup timer on unmount
    return () => {
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current);
      }
    };
  }, [refreshAccessToken, setupRefreshTimer, setUser]);

  return {
    refreshAccessToken,
    setupRefreshTimer,
  };
};
