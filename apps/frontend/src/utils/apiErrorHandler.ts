/**
 * API Error Handler
 * Centralized error handling for API requests
 */

import { AxiosError } from 'axios';
import type { ApiErrorResponse, ApiError } from '../types/api.types';

/**
 * Extract error message from various error formats
 */
export const extractErrorMessage = (error: unknown): string => {
  if (!error) return 'An unexpected error occurred';

  // Axios error with response
  if (isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorResponse>;
    
    if (axiosError.response?.data) {
      const errorData = axiosError.response.data;
      
      // Spring Boot validation errors
      if (errorData.validationErrors && errorData.validationErrors.length > 0) {
        return errorData.validationErrors
          .map(ve => ve.message)
          .join(', ');
      }
      
      // Standard error message
      if (errorData.message) {
        return errorData.message;
      }
      
      // Error field
      if (errorData.error) {
        return errorData.error;
      }
    }
    
    // Network errors
    if (axiosError.code === 'ECONNABORTED') {
      return 'Request timeout. Please try again.';
    }
    
    if (axiosError.code === 'ERR_NETWORK') {
      return 'Network error. Please check your connection.';
    }
    
    // HTTP status messages
    if (axiosError.response?.status) {
      return getStatusMessage(axiosError.response.status);
    }
    
    return axiosError.message || 'Request failed';
  }

  // Error object
  if (error instanceof Error) {
    return error.message;
  }

  // String error
  if (typeof error === 'string') {
    return error;
  }

  return 'An unexpected error occurred';
};

/**
 * Convert error to ApiError format
 */
export const normalizeError = (error: unknown): ApiError => {
  if (!error) {
    return {
      message: 'An unexpected error occurred',
      statusCode: 500,
    };
  }

  if (isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorResponse>;
    
    return {
      message: extractErrorMessage(error),
      statusCode: axiosError.response?.status || 500,
      details: axiosError.response?.data?.details,
      validationErrors: axiosError.response?.data?.validationErrors,
    };
  }

  if (error instanceof Error) {
    return {
      message: error.message,
      statusCode: 500,
    };
  }

  return {
    message: String(error),
    statusCode: 500,
  };
};

/**
 * Check if error is an Axios error
 */
export const isAxiosError = (error: unknown): error is AxiosError => {
  return (error as AxiosError)?.isAxiosError === true;
};

/**
 * Check if error is a network error
 */
export const isNetworkError = (error: unknown): boolean => {
  if (!isAxiosError(error)) return false;
  
  const axiosError = error as AxiosError;
  return (
    axiosError.code === 'ERR_NETWORK' ||
    axiosError.code === 'ECONNABORTED' ||
    !axiosError.response
  );
};

/**
 * Check if error is a server error (5xx)
 */
export const isServerError = (error: unknown): boolean => {
  if (!isAxiosError(error)) return false;
  
  const status = (error as AxiosError).response?.status;
  return status ? status >= 500 && status < 600 : false;
};

/**
 * Check if error is a client error (4xx)
 */
export const isClientError = (error: unknown): boolean => {
  if (!isAxiosError(error)) return false;
  
  const status = (error as AxiosError).response?.status;
  return status ? status >= 400 && status < 500 : false;
};

/**
 * Check if error is an authentication error (401)
 */
export const isAuthError = (error: unknown): boolean => {
  if (!isAxiosError(error)) return false;
  
  return (error as AxiosError).response?.status === 401;
};

/**
 * Check if error is a forbidden error (403)
 */
export const isForbiddenError = (error: unknown): boolean => {
  if (!isAxiosError(error)) return false;
  
  return (error as AxiosError).response?.status === 403;
};

/**
 * Check if error is a validation error (400 with validation errors)
 */
export const isValidationError = (error: unknown): boolean => {
  if (!isAxiosError(error)) return false;
  
  const axiosError = error as AxiosError<ApiErrorResponse>;
  return (
    axiosError.response?.status === 400 &&
    !!axiosError.response?.data?.validationErrors
  );
};

/**
 * Get user-friendly message based on HTTP status code
 */
export const getStatusMessage = (status: number): string => {
  const messages: Record<number, string> = {
    400: 'Bad request. Please check your input.',
    401: 'Authentication required. Please login.',
    403: 'You do not have permission to perform this action.',
    404: 'The requested resource was not found.',
    408: 'Request timeout. Please try again.',
    409: 'Conflict. The resource already exists.',
    422: 'Validation failed. Please check your input.',
    429: 'Too many requests. Please try again later.',
    500: 'Internal server error. Please try again.',
    502: 'Bad gateway. Please try again.',
    503: 'Service unavailable. Please try again later.',
    504: 'Gateway timeout. Please try again.',
  };

  return messages[status] || `Request failed with status ${status}`;
};

/**
 * Log error for debugging (can be extended to send to monitoring service)
 */
export const logError = (
  error: unknown,
  context?: string,
  additionalData?: Record<string, any>
): void => {
  const normalizedError = normalizeError(error);
  
  console.error('API Error:', {
    context,
    error: normalizedError,
    timestamp: new Date().toISOString(),
    ...additionalData,
  });
  
  // TODO: Send to error monitoring service (Sentry, LogRocket, etc.)
};

/**
 * Format validation errors for display
 */
export const formatValidationErrors = (
  errors: Array<{ field: string; message: string }>
): Record<string, string> => {
  return errors.reduce((acc, error) => {
    acc[error.field] = error.message;
    return acc;
  }, {} as Record<string, string>);
};
