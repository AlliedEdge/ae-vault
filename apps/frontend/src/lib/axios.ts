import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { tokenService } from '../services/tokenService';

// Create axios instance with default config for Spring Boot backend
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  timeout: 30000, // 30 seconds
  withCredentials: false, // Set to true if using httpOnly cookies
});

/**
 * Request interceptor - Add JWT token to requests
 * Automatically attaches the access token from memory to every request
 */
axiosInstance.interceptors.request.use(
  (config) => {
    const token = tokenService.getAccessToken();
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor - Handle token refresh and errors
 * 
 * Features:
 * 1. Automatically refreshes expired access tokens using refresh token
 * 2. Retries failed requests after successful token refresh
 * 3. Queues concurrent requests while token is being refreshed
 * 4. Handles token refresh failures and triggers logout
 * 5. Prevents infinite refresh loops
 */

// Token refresh state management
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

/**
 * Process all queued requests after token refresh
 */
const processQueue = (error: unknown = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });

  failedQueue = [];
};

axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Handle non-401 errors or already retried requests
    if (!error.response || error.response.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    // Don't retry auth endpoints to prevent infinite loops
    const url = originalRequest.url || '';
    const isAuthEndpoint = url.includes('/auth/login') || 
                          url.includes('/auth/register') || 
                          url.includes('/auth/refresh');
    
    if (isAuthEndpoint) {
      console.log('[Axios] 401 on auth endpoint, not retrying:', url);
      return Promise.reject(error);
    }

    // If already refreshing, queue this request
    if (isRefreshing) {
      console.log('[Axios] Token refresh in progress, queuing request:', url);
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then(() => {
          // Retry with new token
          const token = tokenService.getAccessToken();
          if (token && originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return axiosInstance(originalRequest);
        })
        .catch((err) => {
          return Promise.reject(err);
        });
    }

    // Mark request as retried and start refresh process
    originalRequest._retry = true;
    isRefreshing = true;

    const refreshToken = tokenService.getRefreshToken();

    if (!refreshToken) {
      console.log('[Axios] No refresh token available, logging out');
      processQueue(new Error('No refresh token'));
      isRefreshing = false;
      
      // Clear tokens and trigger logout
      tokenService.clearTokens();
      window.dispatchEvent(new CustomEvent('auth:token-refresh-failed', { 
        detail: { reason: 'no_refresh_token' } 
      }));
      
      return Promise.reject(error);
    }

    try {
      console.log('[Axios] Attempting to refresh access token...');
      
      // Attempt to refresh the token
      const response = await axios.post(
        `${import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'}/auth/refresh`,
        { refreshToken },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      const { accessToken, refreshToken: newRefreshToken } = response.data;

      // Store new tokens
      tokenService.setTokens(accessToken, newRefreshToken || refreshToken);

      console.log('[Axios] Token refresh successful');

      // Update authorization header for the original request
      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
      }

      // Process all queued requests with new token
      processQueue();
      isRefreshing = false;

      // Retry the original request
      return axiosInstance(originalRequest);
    } catch (refreshError: any) {
      console.error('[Axios] Token refresh failed:', refreshError);
      
      // Process queue with error
      processQueue(refreshError);
      isRefreshing = false;
      
      // Clear tokens
      tokenService.clearTokens();
      
      // Dispatch custom event for logout
      window.dispatchEvent(new CustomEvent('auth:token-refresh-failed', { 
        detail: { 
          reason: 'refresh_failed',
          error: refreshError.response?.data?.message || 'Token refresh failed'
        } 
      }));
      
      return Promise.reject(refreshError);
    }
  }
);

export default axiosInstance;
