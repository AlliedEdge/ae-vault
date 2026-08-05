/**
 * Retry Handler
 * Handles automatic retry logic for failed API requests
 */

import { AxiosError } from 'axios';
import type { RetryConfig } from '../types/api.types';
import { isNetworkError, isServerError } from './apiErrorHandler';

/**
 * Sleep utility for delays
 */
const sleep = (ms: number): Promise<void> => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

/**
 * Calculate exponential backoff delay
 */
export const calculateBackoff = (
  attempt: number,
  baseDelay: number,
  maxDelay: number = 30000
): number => {
  const exponentialDelay = baseDelay * Math.pow(2, attempt);
  const jitter = Math.random() * 1000; // Add jitter to prevent thundering herd
  return Math.min(exponentialDelay + jitter, maxDelay);
};

/**
 * Check if error is retryable
 */
export const isRetryableError = (
  error: unknown,
  retryableStatusCodes: number[]
): boolean => {
  // Always retry network errors
  if (isNetworkError(error)) {
    return true;
  }

  // Retry server errors
  if (isServerError(error)) {
    return true;
  }

  // Check if status code is in retryable list
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as AxiosError;
    const status = axiosError.response?.status;
    if (status && retryableStatusCodes.includes(status)) {
      return true;
    }
  }

  return false;
};

/**
 * Execute function with retry logic
 */
export const withRetry = async <T>(
  fn: () => Promise<T>,
  config: RetryConfig
): Promise<T> => {
  const {
    maxRetries,
    retryDelay,
    retryableStatusCodes,
    shouldRetry: customShouldRetry,
  } = config;

  let lastError: unknown;
  
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      // Execute the function
      const result = await fn();
      return result;
    } catch (error) {
      lastError = error;
      
      // Check if we should retry
      const shouldRetry = customShouldRetry
        ? customShouldRetry(error)
        : isRetryableError(error, retryableStatusCodes);
      
      // If this is the last attempt or error is not retryable, throw
      if (attempt === maxRetries || !shouldRetry) {
        throw error;
      }
      
      // Calculate delay for next attempt
      const delay = calculateBackoff(attempt, retryDelay);
      
      console.warn(
        `Request failed (attempt ${attempt + 1}/${maxRetries + 1}). ` +
        `Retrying in ${Math.round(delay / 1000)}s...`,
        error
      );
      
      // Wait before retrying
      await sleep(delay);
    }
  }
  
  // Should never reach here, but TypeScript needs it
  throw lastError;
};

/**
 * Create a retry decorator for async functions
 */
export const createRetryDecorator = (config: RetryConfig) => {
  return <T extends (...args: any[]) => Promise<any>>(fn: T): T => {
    return (async (...args: Parameters<T>) => {
      return withRetry(() => fn(...args), config);
    }) as T;
  };
};

/**
 * Check if we should retry based on error type and attempt count
 */
export const shouldRetryRequest = (
  error: unknown,
  attempt: number,
  maxRetries: number,
  config?: Partial<RetryConfig>
): boolean => {
  // Exceeded max retries
  if (attempt >= maxRetries) {
    return false;
  }

  // Custom retry logic if provided
  if (config?.shouldRetry) {
    return config.shouldRetry(error);
  }

  // Default retry logic
  const retryableStatusCodes = config?.retryableStatusCodes || [408, 429, 500, 502, 503, 504];
  return isRetryableError(error, retryableStatusCodes);
};

/**
 * Get retry attempt information for logging
 */
export const getRetryInfo = (
  attempt: number,
  maxRetries: number,
  delay: number
): string => {
  return `Retry ${attempt}/${maxRetries} in ${Math.round(delay / 1000)}s`;
};
