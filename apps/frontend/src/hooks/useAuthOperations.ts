/**
 * useAuthOperations Hook
 * Provides auth operations with loading and error states
 */

import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authService } from '../services/authService';
import { useApi } from './useApi';
import type {
  LoginRequestDto,
  RegisterRequestDto,
  ForgotPasswordRequestDto,
  ResetPasswordRequestDto,
} from '../types/api.types';

export const useAuthOperations = () => {
  const navigate = useNavigate();
  const store = useAuthStore();

  // Individual operation states
  const forgotPassword = useApi(authService.forgotPassword);
  const resetPassword = useApi(authService.resetPassword);
  const verifyEmail = useApi(authService.verifyEmail);
  const resendVerification = useApi(authService.resendVerification);

  /**
   * Login with navigation
   */
  const login = useCallback(
    async (credentials: LoginRequestDto, redirectTo?: string) => {
      try {
        await store.login(credentials);
        navigate(redirectTo || '/', { replace: true });
      } catch (error) {
        // Error is already handled by the store
        throw error;
      }
    },
    [store, navigate]
  );

  /**
   * Register with navigation
   */
  const register = useCallback(
    async (data: RegisterRequestDto, redirectTo?: string) => {
      try {
        await store.register(data);
        navigate(redirectTo || '/', { replace: true });
      } catch (error) {
        // Error is already handled by the store
        throw error;
      }
    },
    [store, navigate]
  );

  /**
   * Logout with navigation
   */
  const logout = useCallback(async () => {
    try {
      await store.logout();
      navigate('/login', { replace: true });
    } catch (error) {
      // Error is already handled by the store
      throw error;
    }
  }, [store, navigate]);

  /**
   * Handle forgot password
   */
  const handleForgotPassword = useCallback(
    async (data: ForgotPasswordRequestDto) => {
      try {
        const result = await forgotPassword.execute(data);
        return result;
      } catch (error) {
        throw error;
      }
    },
    [forgotPassword]
  );

  /**
   * Handle reset password
   */
  const handleResetPassword = useCallback(
    async (data: ResetPasswordRequestDto) => {
      try {
        const result = await resetPassword.execute(data);
        // Navigate to login after successful password reset
        setTimeout(() => {
          navigate('/login', { 
            replace: true,
            state: { message: 'Password reset successful. Please login with your new password.' }
          });
        }, 2000);
        return result;
      } catch (error) {
        throw error;
      }
    },
    [resetPassword, navigate]
  );

  /**
   * Handle email verification
   */
  const handleVerifyEmail = useCallback(
    async (token: string) => {
      try {
        const result = await verifyEmail.execute(token);
        // Refresh auth to get updated user data
        await store.refreshAuth();
        return result;
      } catch (error) {
        throw error;
      }
    },
    [verifyEmail, store]
  );

  /**
   * Handle resend verification
   */
  const handleResendVerification = useCallback(
    async (email: string) => {
      try {
        const result = await resendVerification.execute(email);
        return result;
      } catch (error) {
        throw error;
      }
    },
    [resendVerification]
  );

  return {
    // Main auth operations
    login,
    register,
    logout,
    
    // Password operations
    forgotPassword: {
      handleExecute: handleForgotPassword,
      isLoading: forgotPassword.isLoading,
      error: forgotPassword.error,
      data: forgotPassword.data,
      isSuccess: forgotPassword.isSuccess,
      reset: forgotPassword.reset,
    },
    resetPassword: {
      handleExecute: handleResetPassword,
      isLoading: resetPassword.isLoading,
      error: resetPassword.error,
      data: resetPassword.data,
      isSuccess: resetPassword.isSuccess,
      reset: resetPassword.reset,
    },
    
    // Email operations
    verifyEmail: {
      handleExecute: handleVerifyEmail,
      isLoading: verifyEmail.isLoading,
      error: verifyEmail.error,
      data: verifyEmail.data,
      isSuccess: verifyEmail.isSuccess,
      reset: verifyEmail.reset,
    },
    resendVerification: {
      handleExecute: handleResendVerification,
      isLoading: resendVerification.isLoading,
      error: resendVerification.error,
      data: resendVerification.data,
      isSuccess: resendVerification.isSuccess,
      reset: resendVerification.reset,
    },
    
    // Store state
    isLoading: store.isLoading,
    error: store.error,
    successMessage: store.successMessage,
    user: store.user,
    isAuthenticated: store.isAuthenticated,
    
    // Loading states
    loadingStates: store.loadingStates,
    
    // Utility functions
    clearError: store.clearError,
    clearSuccess: store.clearSuccess,
  };
};
