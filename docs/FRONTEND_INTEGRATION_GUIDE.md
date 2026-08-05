# Frontend Integration Guide - Refresh Token

## Overview

This guide helps frontend developers integrate the refresh token functionality into their applications.

## Quick Start

### 1. Store Tokens Securely

**Web (React/Vue/Angular)**:
```typescript
// Store tokens after login
const storeTokens = (accessToken: string, refreshToken: string) => {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
};

// Retrieve tokens
const getAccessToken = () => localStorage.getItem('accessToken');
const getRefreshToken = () => localStorage.getItem('refreshToken');

// Clear tokens on logout
const clearTokens = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};
```

**Mobile (React Native)**:
```typescript
import AsyncStorage from '@react-native-async-storage/async-storage';

const storeTokens = async (accessToken: string, refreshToken: string) => {
  await AsyncStorage.multiSet([
    ['accessToken', accessToken],
    ['refreshToken', refreshToken]
  ]);
};
```

### 2. API Client Setup

**Using Axios (Recommended)**:

```typescript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Add access token to requests
apiClient.interceptors.request.use(
  (config) => {
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle token refresh
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

const subscribeTokenRefresh = (cb: (token: string) => void) => {
  refreshSubscribers.push(cb);
};

const onTokenRefreshed = (token: string) => {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If error is 401 and we haven't retried yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Wait for refresh to complete
        return new Promise((resolve) => {
          subscribeTokenRefresh((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(apiClient(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        // Call refresh endpoint
        const response = await axios.post(
          `${API_BASE_URL}/auth/refresh`,
          { refreshToken }
        );

        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = 
          response.data.data;

        // Store new tokens
        localStorage.setItem('accessToken', newAccessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        // Update authorization header
        apiClient.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

        // Notify waiting requests
        onTokenRefreshed(newAccessToken);

        // Retry original request
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed - redirect to login
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

**Using Fetch API**:

```typescript
let isRefreshing = false;
let refreshPromise: Promise<string> | null = null;

const refreshAccessToken = async (): Promise<string> => {
  const refreshToken = localStorage.getItem('refreshToken');
  
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
    throw new Error('Token refresh failed');
  }

  const data = await response.json();
  const { accessToken, refreshToken: newRefreshToken } = data.data;

  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', newRefreshToken);

  return accessToken;
};

const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
  let accessToken = localStorage.getItem('accessToken');

  // Add authorization header
  const headers = {
    ...options.headers,
    Authorization: `Bearer ${accessToken}`,
  };

  let response = await fetch(url, { ...options, headers });

  // If 401, try to refresh token
  if (response.status === 401) {
    if (!isRefreshing) {
      isRefreshing = true;
      refreshPromise = refreshAccessToken().finally(() => {
        isRefreshing = false;
        refreshPromise = null;
      });
    }

    // Wait for refresh to complete
    accessToken = await refreshPromise!;

    // Retry request with new token
    response = await fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        Authorization: `Bearer ${accessToken}`,
      },
    });
  }

  return response;
};

export { fetchWithAuth };
```

## Usage Examples

### Login Flow

```typescript
import apiClient from './apiClient';

interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: number;
    username: string;
    email: string;
    role: string;
  };
}

const login = async (credentials: LoginRequest): Promise<AuthResponse> => {
  try {
    const response = await apiClient.post('/auth/login', credentials);
    
    const authData = response.data.data;
    
    // Store tokens
    localStorage.setItem('accessToken', authData.accessToken);
    localStorage.setItem('refreshToken', authData.refreshToken);
    
    return authData;
  } catch (error) {
    console.error('Login failed:', error);
    throw error;
  }
};

// Usage in component
const handleLogin = async (username: string, password: string) => {
  try {
    const authData = await login({ usernameOrEmail: username, password });
    console.log('Logged in:', authData.user);
    // Redirect to dashboard
    navigate('/dashboard');
  } catch (error) {
    setError('Invalid credentials');
  }
};
```

### Logout Flow

```typescript
const logout = async () => {
  try {
    const accessToken = localStorage.getItem('accessToken');
    
    // Call backend logout endpoint
    await apiClient.post('/auth/logout', {}, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
  } catch (error) {
    console.error('Logout request failed:', error);
  } finally {
    // Clear tokens regardless of API result
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    
    // Redirect to login
    window.location.href = '/login';
  }
};
```

### Protected API Calls

```typescript
// Get user profile (protected endpoint)
const getUserProfile = async () => {
  try {
    const response = await apiClient.get('/users/profile');
    return response.data.data;
  } catch (error) {
    console.error('Failed to fetch profile:', error);
    throw error;
  }
};

// Update user profile
const updateProfile = async (profileData: any) => {
  try {
    const response = await apiClient.put('/users/profile', profileData);
    return response.data.data;
  } catch (error) {
    console.error('Failed to update profile:', error);
    throw error;
  }
};
```

## React Hook for Authentication

```typescript
import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import apiClient from './apiClient';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check if user is already logged in
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      // Verify token and get user info
      verifyToken();
    } else {
      setIsLoading(false);
    }
  }, []);

  const verifyToken = async () => {
    try {
      const response = await apiClient.get('/auth/verify');
      setUser(response.data.data.user);
    } catch (error) {
      // Token invalid, clear storage
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (username: string, password: string) => {
    const response = await apiClient.post('/auth/login', {
      usernameOrEmail: username,
      password,
    });

    const { accessToken, refreshToken, user } = response.data.data;
    
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    
    setUser(user);
  };

  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        isAuthenticated: !!user,
        isLoading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

**Usage in Component**:

```typescript
import { useAuth } from './AuthProvider';

function LoginPage() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(username, password);
      // Redirect handled by AuthProvider
    } catch (error) {
      alert('Login failed');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder="Username"
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Password"
      />
      <button type="submit">Login</button>
    </form>
  );
}
```

## Protected Routes

```typescript
import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthProvider';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

// Usage in routes
<Route
  path="/dashboard"
  element={
    <ProtectedRoute>
      <Dashboard />
    </ProtectedRoute>
  }
/>
```

## Token Expiration Handling

### Proactive Refresh (Before Expiration)

```typescript
import { jwtDecode } from 'jwt-decode';

interface TokenPayload {
  exp: number;
  iat: number;
  sub: string;
}

const shouldRefreshToken = (): boolean => {
  const accessToken = localStorage.getItem('accessToken');
  
  if (!accessToken) return false;

  try {
    const decoded = jwtDecode<TokenPayload>(accessToken);
    const now = Date.now() / 1000;
    const timeUntilExpiry = decoded.exp - now;

    // Refresh if less than 2 minutes remaining
    return timeUntilExpiry < 120;
  } catch (error) {
    return true; // Token invalid, should refresh
  }
};

// Check periodically
useEffect(() => {
  const interval = setInterval(async () => {
    if (shouldRefreshToken()) {
      try {
        await refreshAccessToken();
      } catch (error) {
        console.error('Failed to refresh token:', error);
      }
    }
  }, 60000); // Check every minute

  return () => clearInterval(interval);
}, []);
```

## Error Handling

```typescript
enum AuthErrorCode {
  INVALID_TOKEN = 'INVALID_TOKEN',
  TOKEN_EXPIRED = 'TOKEN_EXPIRED',
  REFRESH_FAILED = 'REFRESH_FAILED',
  RATE_LIMIT = 'RATE_LIMIT_EXCEEDED',
}

class AuthError extends Error {
  constructor(public code: AuthErrorCode, message: string) {
    super(message);
    this.name = 'AuthError';
  }
}

// Handle errors in interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      throw new AuthError(
        AuthErrorCode.INVALID_TOKEN,
        'Authentication required'
      );
    }
    
    if (error.response?.status === 429) {
      throw new AuthError(
        AuthErrorCode.RATE_LIMIT,
        'Too many requests. Please try again later.'
      );
    }

    throw error;
  }
);

// Usage
try {
  await getUserProfile();
} catch (error) {
  if (error instanceof AuthError) {
    switch (error.code) {
      case AuthErrorCode.INVALID_TOKEN:
        // Redirect to login
        navigate('/login');
        break;
      case AuthErrorCode.RATE_LIMIT:
        // Show rate limit message
        showToast('Too many requests. Please wait.');
        break;
    }
  }
}
```

## Testing

### Mock API for Development

```typescript
// mockAuth.ts
export const mockAuthService = {
  login: async (username: string, password: string) => {
    // Simulate API delay
    await new Promise((resolve) => setTimeout(resolve, 500));
    
    return {
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      user: {
        id: 1,
        username,
        email: `${username}@example.com`,
        role: 'USER',
      },
    };
  },
  
  refresh: async (refreshToken: string) => {
    await new Promise((resolve) => setTimeout(resolve, 300));
    
    return {
      accessToken: 'new-mock-access-token',
      refreshToken: 'new-mock-refresh-token',
    };
  },
};
```

## Best Practices

### 1. **Security**
- ✅ Store tokens in localStorage (web) or secure storage (mobile)
- ✅ Never log tokens to console
- ✅ Clear tokens on logout
- ✅ Use HTTPS in production
- ❌ Don't store tokens in cookies without httpOnly flag

### 2. **Error Handling**
- ✅ Handle 401 errors gracefully
- ✅ Show user-friendly error messages
- ✅ Implement retry logic for network errors
- ✅ Log errors for debugging

### 3. **Performance**
- ✅ Implement request deduplication
- ✅ Cache user data when possible
- ✅ Use background token refresh
- ✅ Minimize API calls

### 4. **User Experience**
- ✅ Show loading states during auth operations
- ✅ Seamless token refresh (no UI interruption)
- ✅ Clear feedback on auth errors
- ✅ Persist login state across page reloads

## Troubleshooting

### Issue: Infinite refresh loop
**Cause**: Refresh endpoint returning 401
**Solution**: Check that refresh token is valid and not expired

### Issue: Token refresh fails after app restart
**Cause**: Tokens not persisted correctly
**Solution**: Verify localStorage/AsyncStorage operations

### Issue: Multiple refresh requests
**Cause**: Race condition in interceptor
**Solution**: Implement request queuing (see axios example above)

## Production Checklist

- [ ] Update API_BASE_URL to production URL
- [ ] Enable HTTPS
- [ ] Implement secure token storage
- [ ] Add error tracking (Sentry, LogRocket)
- [ ] Test token refresh flow
- [ ] Test logout flow
- [ ] Test expired token handling
- [ ] Test rate limiting response
- [ ] Implement session timeout warning
- [ ] Add loading states
- [ ] Test on multiple devices

## Additional Resources

- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Axios Documentation](https://axios-http.com/docs/intro)
- [React Context API](https://react.dev/learn/passing-data-deeply-with-context)
