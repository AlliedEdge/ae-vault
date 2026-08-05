# JWT Authentication - Quick Reference

Quick guide for developers working with JWT authentication in Ziboto.

## Token Storage

| Token Type | Storage Location | Persistence | Security |
|------------|-----------------|-------------|----------|
| Access Token | In-memory (variable) | Lost on page refresh | High (not in localStorage) |
| Refresh Token | localStorage (encrypted) | Survives page refresh | Medium (encrypted, but in localStorage) |

## How It Works

1. **User logs in** → Backend returns `{ accessToken, refreshToken }`
2. **Tokens stored** → Access token in memory, refresh token in localStorage
3. **API requests** → Access token automatically attached as `Bearer` token
4. **Token expires** → Automatically refreshed using refresh token
5. **Request retried** → Original request retried with new token
6. **Refresh fails** → User logged out and redirected to login

## Common Tasks

### Check if User is Authenticated

```typescript
import { useAuth } from '@/context/AuthContext';

const { isAuthenticated, user } = useAuth();

if (isAuthenticated) {
  console.log('User:', user);
}
```

### Login User

```typescript
import { useAuthOperations } from '@/hooks';

const { login, isLoading, error } = useAuthOperations();

await login({ email, password, rememberMe: true });
// Automatically redirects to home page on success
```

### Logout User

```typescript
import { useAuth } from '@/context/AuthContext';

const { logout } = useAuth();

await logout();
// Automatically redirects to login page
```

### Make Authenticated API Request

```typescript
import axios from '@/lib/axios';

// Token is automatically attached
const response = await axios.get('/users/me');
```

### Manually Refresh Token

```typescript
import { useTokenRefresh } from '@/hooks';

const { refreshAccessToken } = useTokenRefresh();

const success = await refreshAccessToken();
```

### Protect a Route

```typescript
import { ProtectedRoute } from '@/components/auth';

<Route path="/dashboard" element={
  <ProtectedRoute>
    <Dashboard />
  </ProtectedRoute>
} />
```

### Handle Auth Errors

```typescript
import { useAuthOperations } from '@/hooks';

const { login, error, clearError } = useAuthOperations();

try {
  await login(credentials);
} catch (err) {
  console.error('Login failed:', error);
  // Error is automatically set in state
}

// Clear error manually if needed
clearError();
```

## Token Lifecycle Events

### Listen for Token Refresh Failures

```typescript
useEffect(() => {
  const handleRefreshFailed = (event: Event) => {
    const detail = (event as CustomEvent).detail;
    console.log('Token refresh failed:', detail);
  };

  window.addEventListener('auth:token-refresh-failed', handleRefreshFailed);
  
  return () => {
    window.removeEventListener('auth:token-refresh-failed', handleRefreshFailed);
  };
}, []);
```

## Important Files

```
src/
├── services/
│   ├── tokenService.ts        # Token storage and retrieval
│   └── authService.ts         # Auth API calls
├── lib/
│   └── axios.ts               # Axios with JWT interceptors
├── context/
│   └── AuthContext.tsx        # Auth state management
├── hooks/
│   ├── useTokenRefresh.ts     # Token refresh logic
│   └── useAuthOperations.ts   # Auth operations with navigation
└── store/
    └── authStore.ts           # Zustand auth store
```

## Debugging

### Check Tokens in Console

```javascript
// Check if access token exists (in memory)
import { tokenService } from '@/services/tokenService';
console.log('Access token:', tokenService.getAccessToken());

// Check if refresh token exists (in localStorage)
console.log('Refresh token:', tokenService.getRefreshToken());

// Check token expiry
console.log('Token expired:', tokenService.isTokenExpired());
```

### View Tokens in Browser

```javascript
// Open browser console
localStorage.getItem('ziboto_refresh_token'); // Encrypted refresh token
localStorage.getItem('ziboto_token_expiry');  // Token expiry timestamp
```

### Enable Debug Logging

Token service and axios interceptors log important events:
- `[TokenService] ...` - Token storage operations
- `[Axios] ...` - Request/response interceptor actions
- `[useTokenRefresh] ...` - Automatic refresh operations
- `[AuthContext] ...` - Auth context events

## Common Issues

### Issue: User logged out on page refresh

**Cause**: Access token is in memory and cleared on refresh.

**Expected Behavior**: Session should be restored automatically using refresh token.

**Check**:
1. Verify refresh token exists: `tokenService.hasRefreshToken()`
2. Check console for token refresh logs
3. Verify `/auth/refresh` endpoint is working

### Issue: Infinite 401 errors

**Cause**: Token refresh is failing but requests keep retrying.

**Fix**: Check that axios interceptor doesn't retry auth endpoints (`/auth/login`, `/auth/register`, `/auth/refresh`).

### Issue: Token refresh not working

**Check**:
1. Refresh token is valid and not expired
2. Backend `/auth/refresh` endpoint response format matches:
   ```json
   {
     "accessToken": "...",
     "refreshToken": "..."  // Optional - only if rotating
   }
   ```
3. Token service logs show refresh attempt
4. Network tab shows refresh request

## Configuration

### Token Expiry Buffer

Location: `src/services/tokenService.ts`

```typescript
// Default: 1 minute before expiry
return currentTime >= expiryTime - 60 * 1000;
```

### Proactive Refresh Timing

Location: `src/hooks/useTokenRefresh.ts`

```typescript
// Default: 2 minutes before expiry or halfway through token lifetime
const refreshBuffer = Math.min(2 * 60 * 1000, timeUntilExpiry / 2);
```

### Request Timeout

Location: `src/lib/axios.ts`

```typescript
timeout: 30000, // 30 seconds
```

## Security Best Practices

✅ **DO:**
- Use HTTPS in production
- Keep access tokens short-lived (15-30 minutes)
- Implement token rotation on backend
- Use httpOnly cookies for refresh tokens in production
- Validate tokens on backend before processing requests

❌ **DON'T:**
- Store sensitive data in JWT payload
- Use long-lived access tokens
- Skip token validation on backend
- Expose tokens in URLs or logs
- Trust JWT payload without verification

## Testing Checklist

```
[ ] Login with valid credentials
[ ] Login with invalid credentials
[ ] Access protected route when authenticated
[ ] Redirect to login when not authenticated
[ ] Token automatically attached to requests
[ ] Token refresh on 401 response
[ ] Request retried after token refresh
[ ] Logout clears all tokens
[ ] Session restored on page refresh
[ ] Multi-tab logout synchronization
[ ] Token expiry handling
[ ] Network error handling during refresh
```

## API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/auth/login` | POST | Login user |
| `/auth/register` | POST | Register user |
| `/auth/logout` | POST | Logout user |
| `/auth/refresh` | POST | Refresh access token |
| `/users/me` | GET | Get current user profile |

## Need Help?

1. Check [JWT_AUTHENTICATION.md](./JWT_AUTHENTICATION.md) for detailed documentation
2. Review browser console for error messages
3. Check network tab for API requests/responses
4. Verify backend JWT configuration matches frontend expectations
