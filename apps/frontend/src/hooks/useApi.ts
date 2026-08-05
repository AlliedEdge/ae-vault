/**
 * useApi Hook
 * Custom hook for handling API requests with loading, error, and success states
 */

import { useState, useCallback } from 'react';
import { extractErrorMessage, normalizeError } from '../utils/apiErrorHandler';
import type { ApiError } from '../types/api.types';

interface UseApiState<T> {
  data: T | null;
  isLoading: boolean;
  error: string | null;
  apiError: ApiError | null;
  isSuccess: boolean;
  isError: boolean;
}

interface UseApiReturn<T, Args extends any[]> extends UseApiState<T> {
  execute: (...args: Args) => Promise<T>;
  reset: () => void;
  setData: (data: T | null) => void;
  setError: (error: string | null) => void;
}

/**
 * Custom hook for API requests with automatic state management
 * 
 * @example
 * ```tsx
 * const { execute, isLoading, error, data } = useApi(authService.login);
 * 
 * const handleLogin = async (credentials) => {
 *   try {
 *     const result = await execute(credentials);
 *     console.log('Login successful', result);
 *   } catch (error) {
 *     console.error('Login failed');
 *   }
 * };
 * ```
 */
export const useApi = <T, Args extends any[]>(
  apiFunction: (...args: Args) => Promise<T>
): UseApiReturn<T, Args> => {
  const [state, setState] = useState<UseApiState<T>>({
    data: null,
    isLoading: false,
    error: null,
    apiError: null,
    isSuccess: false,
    isError: false,
  });

  const execute = useCallback(
    async (...args: Args): Promise<T> => {
      setState({
        data: null,
        isLoading: true,
        error: null,
        apiError: null,
        isSuccess: false,
        isError: false,
      });

      try {
        const result = await apiFunction(...args);
        
        setState({
          data: result,
          isLoading: false,
          error: null,
          apiError: null,
          isSuccess: true,
          isError: false,
        });

        return result;
      } catch (error: any) {
        const errorMessage = extractErrorMessage(error);
        const normalizedError = normalizeError(error);

        setState({
          data: null,
          isLoading: false,
          error: errorMessage,
          apiError: normalizedError,
          isSuccess: false,
          isError: true,
        });

        throw error;
      }
    },
    [apiFunction]
  );

  const reset = useCallback(() => {
    setState({
      data: null,
      isLoading: false,
      error: null,
      apiError: null,
      isSuccess: false,
      isError: false,
    });
  }, []);

  const setData = useCallback((data: T | null) => {
    setState(prev => ({ ...prev, data }));
  }, []);

  const setError = useCallback((error: string | null) => {
    setState(prev => ({
      ...prev,
      error,
      apiError: error ? { message: error } : null,
      isError: !!error,
    }));
  }, []);

  return {
    ...state,
    execute,
    reset,
    setData,
    setError,
  };
};

/**
 * Hook for managing multiple API requests
 */
export const useApiMultiple = <T extends Record<string, any>>(
  apiFunctions: { [K in keyof T]: (...args: any[]) => Promise<T[K]> }
): {
  [K in keyof T]: UseApiReturn<T[K], any[]>;
} => {
  const results = {} as any;

  for (const key in apiFunctions) {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    results[key] = useApi(apiFunctions[key]);
  }

  return results;
};
