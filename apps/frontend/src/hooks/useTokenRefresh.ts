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

    try {
      isRefreshingRef.current = true;
      console.log('[useTokenRefresh] Refreshing access token...');
      
      const response = await authService.refreshToken(refreshToken);
      
      // Update tokens (only access token if backend doesn't rotate refresh token)
      if (response.refreshToken) {
        tokenService.setTokens(response.accessToken, response.refreshToken);
      } else {
        tokenService.setAccessToken(response.accessToken);
      }
      
      console.log('[useTokenRefresh] Token refresh successful');
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

      // If we have refresh token but no access token, restore session
      if (!hasAccessToken && hasRefreshToken) {
        console.log('[useTokenRefresh] Access token missing, restoring session...');
        const success = await refreshAccessToken();
        
        if (success) {
          setupRefreshTimer();
        }
      } else if (hasAccessToken && hasRefreshToken) {
        // If we have both tokens, just setup the refresh timer
        setupRefreshTimer();
      }
    };

    restoreSession();

    // Cleanup timer on unmount
    return () => {
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current);
      }
    };
  }, [refreshAccessToken, setupRefreshTimer]);

  return {
    refreshAccessToken,
    setupRefreshTimer,
  };
};
