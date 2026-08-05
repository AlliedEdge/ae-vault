# Authentication Implementation Guide

## Overview

This document describes the authentication implementation using **Zustand**, **Axios**, and **React Context**.

## Architecture

```
┌─────────────────┐
│   Components    │
│  (Login, etc.)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌──────────────┐
│  Auth Context   │────▶│  Auth Store  │
│   (Provider)    │     │   (Zustand)  │
└────────┬────────┘     └──────┬───────┘
         │                     │
         │                     ▼
         │              ┌──────────────┐
         │              │ Auth Service │
         │              │  (API Calls) │
         │              └──────┬───────┘
         │                     │
         ▼                     ▼
┌─────────────────┐     ┌──────────────┐
│ Token Service   │     │    Axios     │
│   (Storage)     │     │  (HTTP)      │
└─────────────────┘     └──────────────┘
```

## Components

### 1. Token Service (`src/services/tokenService.ts`)

Manages token storage and retrieval.

**Features:**
- Store/retrieve access and refresh tokens
- Check token expiration
- Decode JWT tokens
- Extract user data from tokens
- Calculate time until expiry

**Methods:**
```typescript
tokenService.getAccessToken(): string | null
tokenService.getRefreshToken(): string | null
tokenService.setTokens(accessToken, refreshToken): void
tokenService.clearTokens(): void
tokenService.isTokenExpired(): boolean
tokenService.hasValidTokens(): boolean
tokenService.getUserFromToken(): any
tokenService.getTimeUntilExpiry(): number
```

**Storage:**
- Uses `localStorage` for persistence
- Keys: `ziboto_access_token`, `ziboto_refresh_token`, `ziboto_token_expiry`

### 2. Axios Instance (`src/lib/axios.ts`)

Configured axios instance with interceptors.

**Features:**
- Automatic token attachment to requests
- Automatic token refresh on 401 errors
- Request/response error handling
- Queue failed requests during token refresh

**Interceptors:**

**Request Interceptor:**
- Adds `Authorization: Bearer <token>` header
- Runs before every HTTP request

**Response Interceptor:**
- Catches 401 Unauthorized errors
- Attempts to refresh token automatically
- Retries original request with new token
- Handles refresh failures gracefully
- Prevents duplicate refresh requests

### 3. Auth Service (`src/services/authService.ts`)

API call layer for authentication.

**Methods:**
```typescript
authService.login(credentials): Promise<AuthResponse>
authService.register(data): Promise<AuthResponse>
authService.logout(): Promise<void>
authService.refreshToken(token): Promise<RefreshTokenResponse>
authService.getProfile(): Promise<User>
authService.forgotPassword(data): Promise<{message: string}>
authService.resetPassword(data): Promise<{message: string}>
authService.verifyEmail(token): Promise<{message: string}>
authService.resendVerification(email): Promise<{message: string}>
```

**Expected API Endpoints:**
```
POST   /api/auth/login
POST   /api/auth/register
POST   /api/auth/logout
POST   /api/auth/refresh
GET    /api/auth/profile
POST   /api/auth/forgot-password
POST   /api/auth/reset-password
POST   /api/auth/verify-email
POST   /api/auth/resend-verification
```

### 4. Auth Store (`src/store/authStore.ts`)

Zustand store for authentication state.

**State:**
```typescript
{
  user: User | null,
  isAuthenticated: boolean,
  isLoading: boolean,
  error: string | null,
  isInitialized: boolean,
}
```

**Actions:**
```typescript
login(credentials): Promise<void>
register(data): Promise<void>
logout(): Promise<void>
refreshAuth(): Promise<void>
checkAuth(): Promise<void>
clearError(): void
setUser(user): void
```

**Features:**
- Persists user and isAuthenticated state
- DevTools integration for debugging
- Type-safe with TypeScript

### 5. Auth Context (`src/context/AuthContext.tsx`)

React Context Provider for auth features.

**Features:**
- Initializes authentication on app load
- Monitors session expiration
- Auto-logout when token expires
- Cross-tab logout synchronization
- Session expiration redirect

**Provides:**
```typescript
{
  isAuthenticated: boolean,
  isLoading: boolean,
  user: any,
  error: string | null,
  checkAuth(): Promise<void>,
  logout(): Promise<void>,
  clearError(): void,
}
```

### 6. Route Guards

**ProtectedRoute** (`src/components/auth/ProtectedRoute.tsx`)
- Redirects unauthenticated users to login
- Shows loading state during auth check
- Preserves intended destination

**PublicRoute** (`src/components/auth/PublicRoute.tsx`)
- Redirects authenticated users away from auth pages
- Prevents logged-in users from seeing login/register

## Authentication Flow

### Login Flow

```
1. User submits login form
   ↓
2. Login component calls authStore.login()
   ↓
3. Store calls authService.login()
   ↓
4. Axios sends POST to /api/auth/login
   ↓
5. Backend validates credentials
   ↓
6. Backend returns { user, accessToken, refreshToken }
   ↓
7. Store saves tokens via tokenService
   ↓
8. Store updates state { user, isAuthenticated: true }
   ↓
9. User is redirected to home page
```

### Auto-Login Flow

```
1. App starts
   ↓
2. AuthProvider calls checkAuth()
   ↓
3. Store checks for valid tokens via tokenService
   ↓
4. If tokens exist and not expired:
   - Fetch user profile from API
   - Set isAuthenticated = true
   ↓
5. If tokens missing/expired:
   - Clear tokens
   - Set isAuthenticated = false
   - User sees login page
```

### Token Refresh Flow

```
1. API request returns 401 Unauthorized
   ↓
2. Axios response interceptor catches error
   ↓
3. Check if refresh already in progress
   - If yes: Queue this request
   - If no: Start refresh process
   ↓
4. POST refresh token to /api/auth/refresh
   ↓
5. Backend validates refresh token
   ↓
6. Backend returns new { accessToken, refreshToken }
   ↓
7. Save new tokens via tokenService
   ↓
8. Retry original request with new token
   ↓
9. Process queued requests with new token
   ↓
10. If refresh fails:
    - Clear all tokens
    - Redirect to session expired page
```

### Logout Flow

```
1. User clicks logout button
   ↓
2. Component calls authStore.logout()
   ↓
3. Store calls authService.logout() (optional API call)
   ↓
4. Clear tokens via tokenService
   ↓
5. Reset store state
   ↓
6. Redirect to login page
```

### Session Expiration

```
1. AuthContext checks token expiry every 60 seconds
   ↓
2. When token is about to expire:
   - Clear tokens
   - Call logout
   - Redirect to /session-expired
   ↓
3. Session expired page shows countdown
   ↓
4. Auto-redirect to login after countdown
```

## API Response Format

### Login/Register Response

```json
{
  "user": {
    "id": "user-id",
    "email": "user@example.com",
    "name": "John Doe",
    "role": "user",
    "emailVerified": true
  },
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

### Refresh Token Response

```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

### Error Response

```json
{
  "message": "Invalid credentials",
  "statusCode": 401,
  "error": "Unauthorized"
}
```

## JWT Token Structure

### Access Token Payload

```json
{
  "sub": "user-id",
  "email": "user@example.com",
  "name": "John Doe",
  "role": "user",
  "iat": 1234567890,
  "exp": 1234571490
}
```

**Note:** The `exp` (expiration) timestamp should be set by your backend.

## Configuration

### Environment Variables

Create `.env` file:

```env
VITE_API_URL=http://localhost:3000/api
```

### Token Expiration

**Recommended Settings:**
- Access Token: 15 minutes
- Refresh Token: 7 days
- Consider token expired: 5 minutes before actual expiry

## Security Features

1. **Token Storage**
   - Tokens stored in localStorage
   - Alternative: Use httpOnly cookies (requires backend changes)

2. **Automatic Refresh**
   - Tokens refreshed before expiration
   - Failed requests retried with new token

3. **Cross-Tab Logout**
   - Logout in one tab logs out all tabs
   - Uses storage events

4. **Session Monitoring**
   - Checks token expiry every minute
   - Auto-logout on expiration

5. **Request Queueing**
   - Prevents duplicate refresh requests
   - Queues requests during token refresh

## Usage Examples

### Using Auth in Components

```tsx
import { useAuth } from '../context/AuthContext';

function MyComponent() {
  const { user, isAuthenticated, logout } = useAuth();

  if (!isAuthenticated) {
    return <div>Please login</div>;
  }

  return (
    <div>
      <p>Welcome, {user.name}!</p>
      <button onClick={logout}>Logout</button>
    </div>
  );
}
```

### Using Auth Store Directly

```tsx
import { useAuthStore } from '../store/authStore';

function MyComponent() {
  const user = useAuthStore((state) => state.user);
  const login = useAuthStore((state) => state.login);
  const error = useAuthStore((state) => state.error);

  // Use login, user, error...
}
```

### Making Authenticated API Calls

```tsx
import axios from '../lib/axios';

async function fetchUserData() {
  try {
    const response = await axios.get('/user/profile');
    return response.data;
  } catch (error) {
    console.error('Failed to fetch user data:', error);
  }
}
```

### Protected Route

```tsx
<Route
  path="/dashboard"
  element={
    <ProtectedRoute>
      <Dashboard />
    </ProtectedRoute>
  }
/>
```

## Testing

### Test Scenarios

1. **Login Success**
   - Valid credentials → User logged in
   - Redirected to home page

2. **Login Failure**
   - Invalid credentials → Error message shown
   - User stays on login page

3. **Auto-Login**
   - Valid tokens in storage → Auto logged in
   - Invalid/expired tokens → Redirected to login

4. **Token Refresh**
   - API returns 401 → Token refreshed automatically
   - Request retried with new token

5. **Session Expiration**
   - Token expires → Auto logout
   - Redirected to session expired page

6. **Logout**
   - Logout clicked → Tokens cleared
   - Redirected to login page

7. **Cross-Tab Logout**
   - Logout in Tab A → Tab B also logs out

## Troubleshooting

### Issue: Infinite redirect loop

**Cause:** Protected route redirecting to login, login redirecting back

**Solution:** Ensure PublicRoute wraps login/register pages

### Issue: Token not being sent

**Cause:** Axios interceptor not configured

**Solution:** Import axios from `lib/axios`, not from `axios` package

### Issue: Session expiring too early

**Cause:** Token expiry calculation incorrect

**Solution:** Check `TOKEN_EXPIRY_KEY` value in localStorage

### Issue: CORS errors

**Cause:** Backend not configured for CORS

**Solution:** Configure CORS in backend to accept requests from frontend origin

## Best Practices

1. **Always use the configured axios instance**
   ```tsx
   import axios from '../lib/axios'; // ✅ Correct
   import axios from 'axios';        // ❌ Wrong
   ```

2. **Handle errors in components**
   ```tsx
   try {
     await login(credentials);
   } catch (error) {
     // Error already in store.error
     console.error(error);
   }
   ```

3. **Clear errors when needed**
   ```tsx
   useEffect(() => {
     clearError(); // Clear previous errors
   }, []);
   ```

4. **Check authentication before protected actions**
   ```tsx
   if (!isAuthenticated) {
     navigate('/login');
     return;
   }
   ```

5. **Use loading states**
   ```tsx
   if (isLoading) {
     return <LoadingSpinner />;
   }
   ```

## Future Enhancements

- [ ] Add remember me functionality (longer token expiry)
- [ ] Implement two-factor authentication (2FA)
- [ ] Add social login (Google, GitHub)
- [ ] Implement magic link login
- [ ] Add device management
- [ ] Implement login history
- [ ] Add account recovery options
- [ ] Implement rate limiting feedback
- [ ] Add CAPTCHA for security
- [ ] Support multiple user roles/permissions

## Resources

- [Zustand Documentation](https://github.com/pmndrs/zustand)
- [Axios Documentation](https://axios-http.com/docs/intro)
- [JWT.io](https://jwt.io) - JWT Debugger
- [React Context API](https://react.dev/reference/react/useContext)
