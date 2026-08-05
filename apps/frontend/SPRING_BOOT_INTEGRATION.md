# Spring Boot Backend Integration

This document describes the frontend integration with the Spring Boot REST API backend.

## Overview

The frontend is fully integrated with Spring Boot backend endpoints using a robust API service layer with:
- Type-safe DTOs
- Centralized error handling
- Automatic retry logic
- Loading and success states
- Token-based authentication with auto-refresh

## Backend Endpoints

### Authentication Endpoints

All authentication endpoints are prefixed with `/api/v1/auth`:

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| POST | `/api/v1/auth/register` | Register new user | `{ name, email, password }` | `AuthResponseDto` |
| POST | `/api/v1/auth/login` | Login user | `{ email, password, rememberMe? }` | `AuthResponseDto` |
| POST | `/api/v1/auth/refresh` | Refresh access token | `{ refreshToken }` | `RefreshTokenResponseDto` |
| POST | `/api/v1/auth/logout` | Logout user | - | `void` |
| POST | `/api/v1/auth/forgot-password` | Request password reset | `{ email }` | `MessageResponseDto` |
| POST | `/api/v1/auth/reset-password` | Reset password | `{ token, newPassword }` | `MessageResponseDto` |
| POST | `/api/v1/auth/verify-email` | Verify email | `{ token }` | `MessageResponseDto` |
| POST | `/api/v1/auth/resend-verification` | Resend verification email | `{ email }` | `MessageResponseDto` |

### User Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/v1/users/me` | Get current user profile | `UserProfileResponseDto` |

## Architecture

### 1. Type Definitions (`src/types/api.types.ts`)

Type-safe DTOs matching Spring Boot response structures:

```typescript
// Request DTOs
interface LoginRequestDto {
  email: string;
  password: string;
  rememberMe?: boolean;
}

interface RegisterRequestDto {
  name: string;
  email: string;
  password: string;
}

// Response DTOs
interface AuthResponseDto {
  user: UserDto;
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
}

interface UserDto {
  id: string;
  email: string;
  name: string;
  role?: string;
  emailVerified?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
```

### 2. API Service Layer (`src/services/authService.ts`)

Centralized service handling all API calls:

```typescript
import { authService } from '../services/authService';

// Login
const response = await authService.login({ email, password });

// Register
const response = await authService.register({ name, email, password });

// Get profile
const user = await authService.getProfile();

// Logout
await authService.logout();

// Refresh token (handled automatically by axios interceptor)
const response = await authService.refreshToken(refreshToken);
```

### 3. Error Handling (`src/utils/apiErrorHandler.ts`)

Comprehensive error handling utilities:

```typescript
import { extractErrorMessage, normalizeError } from '../utils/apiErrorHandler';

try {
  await authService.login(credentials);
} catch (error) {
  const message = extractErrorMessage(error); // User-friendly message
  const normalized = normalizeError(error);   // Structured error object
}
```

Error types handled:
- Network errors (timeout, connection failure)
- HTTP errors (4xx, 5xx)
- Validation errors from Spring Boot
- Authentication errors

### 4. Retry Logic (`src/utils/retryHandler.ts`)

Automatic retry with exponential backoff:

```typescript
import { withRetry } from '../utils/retryHandler';

const result = await withRetry(
  () => authService.login(credentials),
  {
    maxRetries: 3,
    retryDelay: 1000,
    retryableStatusCodes: [408, 429, 500, 502, 503, 504],
  }
);
```

Features:
- Exponential backoff with jitter
- Configurable retry conditions
- Automatic retry for network/server errors
- No retry for client errors (4xx)

### 5. State Management (`src/store/authStore.ts`)

Zustand store with granular loading states:

```typescript
const {
  user,
  isAuthenticated,
  isLoading,
  error,
  successMessage,
  loadingStates, // { login, register, logout, refresh, profile }
  login,
  register,
  logout,
  refreshAuth,
  checkAuth,
} = useAuthStore();
```

### 6. Custom Hooks

#### `useApi` Hook

Generic hook for API operations:

```typescript
import { useApi } from '../hooks/useApi';

const { execute, isLoading, error, data, isSuccess } = useApi(authService.login);

const handleLogin = async () => {
  try {
    const result = await execute(credentials);
    console.log('Success:', result);
  } catch (error) {
    console.error('Error:', error);
  }
};
```

#### `useAuthOperations` Hook

Auth-specific operations:

```typescript
import { useAuthOperations } from '../hooks/useAuthOperations';

const {
  login,
  register,
  logout,
  forgotPassword,
  resetPassword,
  verifyEmail,
  resendVerification,
  isLoading,
  error,
  successMessage,
} = useAuthOperations();
```

### 7. Axios Configuration (`src/lib/axios.ts`)

Pre-configured axios instance with:
- Base URL: `http://localhost:8080/api/v1` (configurable via `VITE_API_URL`)
- Automatic token injection
- Automatic token refresh on 401 errors
- Request/response interceptors

Token refresh flow:
1. Request fails with 401
2. Interceptor catches error
3. Calls `/api/v1/auth/refresh` with refresh token
4. Updates stored tokens
5. Retries original request with new token
6. All queued requests proceed

## Environment Configuration

Create a `.env` file in the frontend directory:

```env
# Backend API URL
VITE_API_URL=http://localhost:8080/api/v1

# Optional: Enable API debug logging
VITE_API_DEBUG=true
```

For production:

```env
VITE_API_URL=https://api.yourdomain.com/api/v1
```

## Token Management

### Storage

Tokens are stored in `localStorage`:
- `ziboto_access_token` - JWT access token
- `ziboto_refresh_token` - Refresh token
- `ziboto_token_expiry` - Token expiration timestamp

### Auto-Refresh

Tokens are automatically refreshed when:
- A 401 error is received
- Token is detected as expired (5 minutes before actual expiry)

### Session Expiration

The app monitors token expiration and:
- Checks every 60 seconds if token is expired
- Automatically logs out when token expires
- Redirects to `/session-expired` page
- Syncs logout across browser tabs

## Error Handling

### Error Display

Errors are displayed in the UI with user-friendly messages:

```typescript
// Spring Boot validation errors
{
  "message": "Validation failed",
  "validationErrors": [
    { "field": "email", "message": "Email is already in use" },
    { "field": "password", "message": "Password is too weak" }
  ]
}

// Displayed as: "Email is already in use, Password is too weak"
```

### Error Types

| Status Code | Meaning | User Message |
|-------------|---------|--------------|
| 400 | Bad Request | "Bad request. Please check your input." |
| 401 | Unauthorized | "Authentication required. Please login." |
| 403 | Forbidden | "You do not have permission to perform this action." |
| 404 | Not Found | "The requested resource was not found." |
| 408 | Request Timeout | "Request timeout. Please try again." |
| 409 | Conflict | "Conflict. The resource already exists." |
| 422 | Validation Error | Specific validation messages |
| 429 | Too Many Requests | "Too many requests. Please try again later." |
| 500 | Server Error | "Internal server error. Please try again." |
| 502 | Bad Gateway | "Bad gateway. Please try again." |
| 503 | Service Unavailable | "Service unavailable. Please try again later." |
| 504 | Gateway Timeout | "Gateway timeout. Please try again." |

## Usage Examples

### Login Flow

```typescript
import { useAuthOperations } from '../hooks/useAuthOperations';

const LoginPage = () => {
  const { login, isLoading, error } = useAuthOperations();

  const handleSubmit = async (data) => {
    try {
      await login(data, '/dashboard'); // Auto-redirect on success
    } catch (error) {
      // Error is already in the store
      console.error('Login failed');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <ErrorMessage>{error}</ErrorMessage>}
      <Input name="email" />
      <Input name="password" type="password" />
      <Button type="submit" isLoading={isLoading}>
        Login
      </Button>
    </form>
  );
};
```

### Register Flow

```typescript
const RegisterPage = () => {
  const { register, loadingStates, error } = useAuthOperations();

  const handleSubmit = async (data) => {
    try {
      await register(data, '/'); // Auto-redirect on success
    } catch (error) {
      // Error handling
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <ErrorMessage>{error}</ErrorMessage>}
      <Input name="name" />
      <Input name="email" />
      <Input name="password" type="password" />
      <Button type="submit" isLoading={loadingStates.register}>
        Create Account
      </Button>
    </form>
  );
};
```

### Forgot Password Flow

```typescript
const ForgotPasswordPage = () => {
  const { forgotPassword } = useAuthOperations();

  const handleSubmit = async (data) => {
    try {
      await forgotPassword.execute(data);
      // Show success message
    } catch (error) {
      // Error is in forgotPassword.error
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {forgotPassword.error && <ErrorMessage>{forgotPassword.error}</ErrorMessage>}
      <Input name="email" />
      <Button type="submit" isLoading={forgotPassword.isLoading}>
        Send Reset Link
      </Button>
    </form>
  );
};
```

### Protected Routes

```typescript
import { useAuth } from '../context/AuthContext';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) return <LoadingSpinner />;
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
};
```

## Testing the Integration

### 1. Start Backend

```bash
cd apps/backend
./mvnw spring-boot:run
```

Backend should be running on `http://localhost:8080`

### 2. Start Frontend

```bash
cd apps/frontend
npm run dev
```

Frontend should be running on `http://localhost:5173`

### 3. Test Endpoints

Use the frontend UI to test:
1. Register a new account
2. Login with credentials
3. View profile (auto-fetched)
4. Logout
5. Forgot password flow
6. Reset password flow

## Common Issues

### CORS Errors

If you see CORS errors, ensure Spring Boot backend has CORS configured:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

### 401 Errors on Refresh

Ensure refresh token endpoint returns new tokens:

```java
@PostMapping("/refresh")
public ResponseEntity<RefreshTokenResponseDto> refresh(@RequestBody RefreshTokenRequestDto request) {
    // Validate refresh token
    // Generate new access token
    // Optionally generate new refresh token
    return ResponseEntity.ok(new RefreshTokenResponseDto(accessToken, refreshToken));
}
```

### Token Not Persisting

Check browser console for localStorage errors. Some browsers block localStorage in private/incognito mode.

## Next Steps

1. Implement Spring Boot backend endpoints
2. Configure CORS on backend
3. Implement JWT token generation
4. Add email verification logic
5. Add password reset logic
6. Add rate limiting
7. Add request validation
8. Add security headers
9. Configure production environment variables
10. Add monitoring and logging

## Additional Resources

- [Spring Boot Security Documentation](https://spring.io/guides/gs/securing-web/)
- [JWT.io](https://jwt.io/)
- [Axios Documentation](https://axios-http.com/)
- [Zustand Documentation](https://docs.pmnd.rs/zustand)
