import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { authService, type User, type LoginCredentials, type RegisterData } from '../services/authService';
import { tokenService } from '../services/tokenService';
import { extractErrorMessage } from '../utils/apiErrorHandler';

interface AuthState {
  // State
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  isInitialized: boolean;
  successMessage: string | null;

  // Loading states for specific operations
  loadingStates: {
    login: boolean;
    register: boolean;
    logout: boolean;
    refresh: boolean;
    profile: boolean;
  };

  // Actions
  login: (credentials: LoginCredentials) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
  checkAuth: () => Promise<void>;
  clearError: () => void;
  clearSuccess: () => void;
  setUser: (user: User | null) => void;
  setError: (error: string | null) => void;
  setSuccess: (message: string | null) => void;
}

export const useAuthStore = create<AuthState>()(
  devtools(
    persist(
      (set, get) => ({
        // Initial state
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
        isInitialized: false,
        successMessage: null,
        loadingStates: {
          login: false,
          register: false,
          logout: false,
          refresh: false,
          profile: false,
        },

        /**
         * Login user
         */
        login: async (credentials: LoginCredentials) => {
          set({
            isLoading: true,
            error: null,
            successMessage: null,
            loadingStates: { ...get().loadingStates, login: true },
          });

          try {
            const response = await authService.login(credentials);

            console.log('[AuthStore] Login response:', {
              hasUser: !!response.user,
              hasAccessToken: !!response.accessToken,
              hasRefreshToken: !!response.refreshToken,
              accessTokenLength: response.accessToken?.length || 0,
              refreshTokenLength: response.refreshToken?.length || 0,
            });

            // Store tokens
            tokenService.setTokens(response.accessToken, response.refreshToken);

            // Update state
            set({
              user: response.user,
              isAuthenticated: true,
              isLoading: false,
              error: null,
              successMessage: 'Login successful!',
              loadingStates: { ...get().loadingStates, login: false },
            });
          } catch (error: any) {
            const errorMessage = extractErrorMessage(error);
            
            set({
              user: null,
              isAuthenticated: false,
              isLoading: false,
              error: errorMessage,
              successMessage: null,
              loadingStates: { ...get().loadingStates, login: false },
            });

            throw error;
          }
        },

        /**
         * Register new user
         */
        register: async (data: RegisterData) => {
          set({
            isLoading: true,
            error: null,
            successMessage: null,
            loadingStates: { ...get().loadingStates, register: true },
          });

          try {
            const response = await authService.register(data);

            // Store tokens
            tokenService.setTokens(response.accessToken, response.refreshToken);

            // Update state
            set({
              user: response.user,
              isAuthenticated: true,
              isLoading: false,
              error: null,
              successMessage: 'Registration successful! Welcome aboard!',
              loadingStates: { ...get().loadingStates, register: false },
            });
          } catch (error: any) {
            const errorMessage = extractErrorMessage(error);
            
            set({
              user: null,
              isAuthenticated: false,
              isLoading: false,
              error: errorMessage,
              successMessage: null,
              loadingStates: { ...get().loadingStates, register: false },
            });

            throw error;
          }
        },

        /**
         * Logout user
         */
        logout: async () => {
          set({
            isLoading: true,
            loadingStates: { ...get().loadingStates, logout: true },
          });

          try {
            // Call logout API
            await authService.logout();
          } catch (error) {
            console.error('Logout API error:', error);
            // Continue with logout even if API fails
          } finally {
            // Clear tokens
            tokenService.clearTokens();

            // Reset state
            set({
              user: null,
              isAuthenticated: false,
              isLoading: false,
              error: null,
              successMessage: null,
              loadingStates: { ...get().loadingStates, logout: false },
            });
          }
        },

        /**
         * Refresh authentication (get fresh user data)
         */
        refreshAuth: async () => {
          set({
            loadingStates: { ...get().loadingStates, refresh: true },
          });

          try {
            const user = await authService.getProfile();
            
            set({
              user,
              isAuthenticated: true,
              error: null,
              loadingStates: { ...get().loadingStates, refresh: false },
            });
          } catch (error: any) {
            console.error('Refresh auth error:', error);
            const errorMessage = extractErrorMessage(error);
            
            // If refresh fails, logout user
            tokenService.clearTokens();
            set({
              user: null,
              isAuthenticated: false,
              error: errorMessage,
              loadingStates: { ...get().loadingStates, refresh: false },
            });

            throw error;
          }
        },

        /**
         * Check authentication status on app init
         */
        checkAuth: async () => {
          // Skip if already initialized
          if (get().isInitialized) return;

          set({
            isLoading: true,
            loadingStates: { ...get().loadingStates, profile: true },
          });

          try {
            const hasAccessToken = !!tokenService.getAccessToken();
            const hasRefreshToken = tokenService.hasRefreshToken();
            
            console.log('[AuthStore] checkAuth - hasAccessToken:', hasAccessToken, 'hasRefreshToken:', hasRefreshToken);
            
            // If we have refresh token but no access token, the useTokenRefresh hook will restore it
            // So we mark as initialized and let the hook handle the restoration
            if (!hasAccessToken && hasRefreshToken) {
              console.log('[AuthStore] Has refresh token but no access token - letting useTokenRefresh restore session');
              set({
                user: null,
                isAuthenticated: false,
                isLoading: false,
                error: null,
                isInitialized: true,
                loadingStates: { ...get().loadingStates, profile: false },
              });
              return;
            }
            
            // If we have access token, try to get user profile
            if (hasAccessToken) {
              try {
                const user = await authService.getProfile();
                
                set({
                  user,
                  isAuthenticated: true,
                  isLoading: false,
                  error: null,
                  isInitialized: true,
                  loadingStates: { ...get().loadingStates, profile: false },
                });
              } catch (error: any) {
                console.error('[AuthStore] Failed to get profile, will try token refresh:', error);
                
                // Don't clear tokens - let useTokenRefresh handle it
                set({
                  user: null,
                  isAuthenticated: false,
                  isLoading: false,
                  error: null,
                  isInitialized: true,
                  loadingStates: { ...get().loadingStates, profile: false },
                });
              }
            } else {
              // No tokens at all
              tokenService.clearTokens();
              
              set({
                user: null,
                isAuthenticated: false,
                isLoading: false,
                error: null,
                isInitialized: true,
                loadingStates: { ...get().loadingStates, profile: false },
              });
            }
          } catch (error: any) {
            console.error('Check auth error:', error);
            
            set({
              user: null,
              isAuthenticated: false,
              isLoading: false,
              error: null,
              isInitialized: true,
              loadingStates: { ...get().loadingStates, profile: false },
            });
          }
        },

        /**
         * Clear error message
         */
        clearError: () => {
          set({ error: null });
        },

        /**
         * Clear success message
         */
        clearSuccess: () => {
          set({ successMessage: null });
        },

        /**
         * Set user manually (useful after token refresh)
         */
        setUser: (user: User | null) => {
          set({
            user,
            isAuthenticated: !!user,
          });
        },

        /**
         * Set error manually
         */
        setError: (error: string | null) => {
          set({ error });
        },

        /**
         * Set success message manually
         */
        setSuccess: (message: string | null) => {
          set({ successMessage: message });
        },
      }),
      {
        name: 'auth-storage',
        partialize: (state) => ({
          // Only persist user data, not loading/error states
          user: state.user,
          isAuthenticated: state.isAuthenticated,
        }),
      }
    ),
    { name: 'AuthStore' }
  )
);
