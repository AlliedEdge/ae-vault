# API Integration Quick Reference

## Quick Start

### 1. Configure Environment

```bash
# Copy example env file
cp .env.example .env

# Update API URL in .env
VITE_API_URL=http://localhost:8080/api/v1
```

### 2. Use Auth Operations

```typescript
import { useAuthOperations } from './hooks/useAuthOperations';

const { login, register, logout, error, isLoading } = useAuthOperations();
```

## Common Patterns

### Login

```typescript
const handleLogin = async (credentials) => {
  try {
    await login(credentials, '/dashboard');
    // Auto-redirects on success
  } catch (error) {
    // Error displayed automatically
  }
};
```

### Register

```typescript
const handleRegister = async (data) => {
  try {
    await register(data, '/');
    // Auto-redirects on success
  } catch (error) {
    // Error displayed automatically
  }
};
```

### Forgot Password

```typescript
const { forgotPassword } = useAuthOperations();

const handleForgotPassword = async (email) => {
  try {
    await forgotPassword.execute({ email });
    setShowSuccess(true);
  } catch (error) {
    // Error in forgotPassword.error
  }
};
```

### Reset Password

```typescript
const { resetPassword } = useAuthOperations();

const handleResetPassword = async (token, newPassword) => {
  try {
    await resetPassword.execute({ token, newPassword });
    // Auto-redirects to login after 2s
  } catch (error) {
    // Error in resetPassword.error
  }
};
```

### Get User Profile

```typescript
import { useAuthStore } from './store/authStore';

const user = useAuthStore((state) => state.user);
const refreshAuth = useAuthStore((state) => state.refreshAuth);

// Refresh profile
await refreshAuth();
```

### Logout

```typescript
const { logout } = useAuthOperations();

const handleLogout = async () => {
  await logout();
  // Auto-redirects to /login
};
```

## Loading States

### Global Loading

```typescript
const { isLoading } = useAuthOperations();

{isLoading && <LoadingSpinner />}
```

### Specific Operation Loading

```typescript
const { loadingStates } = useAuthOperations();

<Button isLoading={loadingStates.login}>Login</Button>
<Button isLoading={loadingStates.register}>Register</Button>
<Button isLoading={loadingStates.logout}>Logout</Button>
```

### Per-Operation Loading

```typescript
const { forgotPassword, resetPassword } = useAuthOperations();

<Button isLoading={forgotPassword.isLoading}>Send Link</Button>
<Button isLoading={resetPassword.isLoading}>Reset Password</Button>
```

## Error Handling

### Display Errors

```typescript
const { error, clearError } = useAuthOperations();

{error && (
  <ErrorMessage onClose={clearError}>
    {error}
  </ErrorMessage>
)}
```

### Operation-Specific Errors

```typescript
const { forgotPassword } = useAuthOperations();

{forgotPassword.error && (
  <ErrorMessage>{forgotPassword.error}</ErrorMessage>
)}
```

## Success Messages

```typescript
const { successMessage, clearSuccess } = useAuthOperations();

{successMessage && (
  <SuccessMessage onClose={clearSuccess}>
    {successMessage}
  </SuccessMessage>
)}
```

## Custom API Calls

### Using useApi Hook

```typescript
import { useApi } from './hooks/useApi';
import { authService } from './services/authService';

const { execute, isLoading, error, data, isSuccess } = useApi(
  authService.verifyEmail
);

const handleVerify = async (token) => {
  try {
    const result = await execute(token);
    console.log('Verification result:', result);
  } catch (error) {
    console.error('Verification failed');
  }
};
```

### Direct Service Call

```typescript
import { authService } from './services/authService';

try {
  const result = await authService.getProfile();
  console.log('User:', result);
} catch (error) {
  console.error('Failed to get profile:', error);
}
```

## Protected Routes

```typescript
import { Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
};
```

## Token Management

### Check Authentication

```typescript
import { useAuth } from './context/AuthContext';

const { isAuthenticated, user } = useAuth();

{isAuthenticated && <UserMenu user={user} />}
```

### Manual Token Operations

```typescript
import { tokenService } from './services/tokenService';

// Get tokens
const accessToken = tokenService.getAccessToken();
const refreshToken = tokenService.getRefreshToken();

// Check if expired
const isExpired = tokenService.isTokenExpired();

// Clear tokens
tokenService.clearTokens();

// Get user from token
const user = tokenService.getUserFromToken();
```

## API Error Types

```typescript
import {
  isNetworkError,
  isServerError,
  isAuthError,
  isForbiddenError,
  isValidationError,
} from './utils/apiErrorHandler';

try {
  await authService.login(credentials);
} catch (error) {
  if (isNetworkError(error)) {
    // Handle network error
  } else if (isAuthError(error)) {
    // Handle 401
  } else if (isValidationError(error)) {
    // Handle validation errors
  }
}
```

## Retry Configuration

### Custom Retry

```typescript
import { withRetry } from './utils/retryHandler';

const result = await withRetry(
  () => authService.someOperation(),
  {
    maxRetries: 5,
    retryDelay: 2000,
    retryableStatusCodes: [408, 429, 500, 502, 503, 504],
    shouldRetry: (error) => {
      // Custom retry logic
      return error.status >= 500;
    },
  }
);
```

## Type Definitions

### Request Types

```typescript
import type {
  LoginRequestDto,
  RegisterRequestDto,
  ForgotPasswordRequestDto,
  ResetPasswordRequestDto,
} from './types/api.types';
```

### Response Types

```typescript
import type {
  AuthResponseDto,
  UserDto,
  MessageResponseDto,
  RefreshTokenResponseDto,
  ApiErrorResponse,
} from './types/api.types';
```

## Backend API Endpoints

```
POST   /api/v1/auth/register           - Register new user
POST   /api/v1/auth/login              - Login user
POST   /api/v1/auth/refresh            - Refresh access token
POST   /api/v1/auth/logout             - Logout user
POST   /api/v1/auth/forgot-password    - Request password reset
POST   /api/v1/auth/reset-password     - Reset password with token
POST   /api/v1/auth/verify-email       - Verify email with token
POST   /api/v1/auth/resend-verification - Resend verification email
GET    /api/v1/users/me                - Get current user profile
```

## Testing

### Test Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

### Test Get Profile

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Troubleshooting

### CORS Issues

Backend needs CORS configuration:

```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

### Token Not Refreshing

Check:
1. Refresh token endpoint returns new tokens
2. Tokens are stored in localStorage
3. Axios interceptor is configured

### 401 Errors

Check:
1. Token is being sent in Authorization header
2. Token format: `Bearer <token>`
3. Token is not expired
4. Backend validates tokens correctly

## Environment Variables

```env
# Required
VITE_API_URL=http://localhost:8080/api/v1

# Optional
VITE_API_DEBUG=false
VITE_API_TIMEOUT=30000
```

## File Structure

```
src/
├── services/
│   ├── authService.ts       # API calls
│   └── tokenService.ts      # Token management
├── store/
│   └── authStore.ts         # State management
├── hooks/
│   ├── useApi.ts            # Generic API hook
│   └── useAuthOperations.ts # Auth-specific hook
├── utils/
│   ├── apiErrorHandler.ts   # Error handling
│   └── retryHandler.ts      # Retry logic
├── types/
│   └── api.types.ts         # Type definitions
├── lib/
│   └── axios.ts             # Axios config
└── context/
    └── AuthContext.tsx      # Auth context
```
