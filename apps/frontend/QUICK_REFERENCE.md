# Quick Reference - Authentication System

## Import Statements

```tsx
// Auth Hook (Context)
import { useAuth } from '../context/AuthContext';

// Auth Store (Zustand)
import { useAuthStore } from '../store/authStore';

// Axios Instance
import axios from '../lib/axios';

// Route Guards
import { ProtectedRoute, PublicRoute } from '../components/auth';

// Token Service
import { tokenService } from '../services/tokenService';

// Auth Service
import { authService } from '../services/authService';
```

## Common Patterns

### Using Auth Context
```tsx
function MyComponent() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  
  if (isLoading) return <div>Loading...</div>;
  if (!isAuthenticated) return <div>Please login</div>;
  
  return <div>Welcome {user.name}!</div>;
}
```

### Using Auth Store
```tsx
function LoginForm() {
  const login = useAuthStore((state) => state.login);
  const error = useAuthStore((state) => state.error);
  const clearError = useAuthStore((state) => state.clearError);
  
  const handleSubmit = async (data) => {
    try {
      await login(data);
    } catch (err) {
      // Error already in store
    }
  };
}
```

### Making API Calls
```tsx
// Automatically includes auth token
async function fetchUserData() {
  const response = await axios.get('/user/data');
  return response.data;
}

// With error handling
async function updateProfile(data) {
  try {
    const response = await axios.put('/user/profile', data);
    return response.data;
  } catch (error) {
    console.error('Update failed:', error);
    throw error;
  }
}
```

### Protected Routes
```tsx
<Routes>
  <Route
    path="/dashboard"
    element={
      <ProtectedRoute>
        <Dashboard />
      </ProtectedRoute>
    }
  />
</Routes>
```

### Public Routes
```tsx
<Routes>
  <Route
    path="/login"
    element={
      <PublicRoute redirectTo="/dashboard">
        <Login />
      </PublicRoute>
    }
  />
</Routes>
```

## Auth Store Actions

```tsx
// Login
await login({ email, password, rememberMe });

// Register
await register({ name, email, password });

// Logout
await logout();

// Refresh auth
await refreshAuth();

// Check auth
await checkAuth();

// Clear error
clearError();

// Set user manually
setUser(userData);
```

## Token Service Methods

```tsx
// Get tokens
const accessToken = tokenService.getAccessToken();
const refreshToken = tokenService.getRefreshToken();

// Set tokens
tokenService.setTokens(accessToken, refreshToken);

// Clear tokens
tokenService.clearTokens();

// Check expiry
const isExpired = tokenService.isTokenExpired();
const hasValid = tokenService.hasValidTokens();

// Get user data
const user = tokenService.getUserFromToken();

// Time until expiry (ms)
const timeLeft = tokenService.getTimeUntilExpiry();
```

## Auth Service Methods

```tsx
// Login
const response = await authService.login({
  email: 'user@example.com',
  password: 'password123',
  rememberMe: true
});

// Register
const response = await authService.register({
  name: 'John Doe',
  email: 'user@example.com',
  password: 'SecurePass123!'
});

// Logout
await authService.logout();

// Refresh token
const tokens = await authService.refreshToken(refreshToken);

// Get profile
const user = await authService.getProfile();

// Forgot password
await authService.forgotPassword({ email: 'user@example.com' });

// Reset password
await authService.resetPassword({
  token: 'reset-token',
  password: 'NewPass123!'
});

// Verify email
await authService.verifyEmail('verification-token');

// Resend verification
await authService.resendVerification('user@example.com');
```

## Axios Configuration

```tsx
// Base URL
const apiUrl = import.meta.env.VITE_API_URL;

// Default timeout
30000ms (30 seconds)

// Request Interceptor
Adds: Authorization: Bearer <token>

// Response Interceptor
Handles: 401 errors → automatic token refresh
```

## Environment Variables

```env
VITE_API_URL=http://localhost:3000/api
```

## Type Definitions

```typescript
interface User {
  id: string;
  email: string;
  name: string;
  role?: string;
  emailVerified?: boolean;
  createdAt?: string;
}

interface LoginCredentials {
  email: string;
  password: string;
  rememberMe?: boolean;
}

interface RegisterData {
  name: string;
  email: string;
  password: string;
}

interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
}
```

## Common Scenarios

### Logout Button
```tsx
function LogoutButton() {
  const { logout } = useAuth();
  
  return (
    <button onClick={logout}>
      Logout
    </button>
  );
}
```

### Show User Info
```tsx
function UserProfile() {
  const { user } = useAuth();
  
  return (
    <div>
      <h2>{user?.name}</h2>
      <p>{user?.email}</p>
    </div>
  );
}
```

### Conditional Rendering
```tsx
function Navigation() {
  const { isAuthenticated } = useAuth();
  
  return (
    <nav>
      {isAuthenticated ? (
        <Link to="/dashboard">Dashboard</Link>
      ) : (
        <Link to="/login">Login</Link>
      )}
    </nav>
  );
}
```

### Handle Auth Errors
```tsx
function LoginForm() {
  const login = useAuthStore((state) => state.login);
  const error = useAuthStore((state) => state.error);
  
  useEffect(() => {
    return () => clearError(); // Cleanup
  }, []);
  
  return (
    <form onSubmit={handleSubmit}>
      {error && <div className="error">{error}</div>}
      {/* form fields */}
    </form>
  );
}
```

## Debug Tips

### Check if user is authenticated
```tsx
console.log('Auth:', useAuthStore.getState().isAuthenticated);
```

### Check token expiry
```tsx
console.log('Expired:', tokenService.isTokenExpired());
console.log('Time left:', tokenService.getTimeUntilExpiry());
```

### View current user
```tsx
console.log('User:', useAuthStore.getState().user);
```

### Check tokens
```tsx
console.log('Access:', tokenService.getAccessToken());
console.log('Refresh:', tokenService.getRefreshToken());
```

## File Locations

```
Auth Context:     src/context/AuthContext.tsx
Auth Store:       src/store/authStore.ts
Token Service:    src/services/tokenService.ts
Auth Service:     src/services/authService.ts
Axios Instance:   src/lib/axios.ts
Protected Route:  src/components/auth/ProtectedRoute.tsx
Public Route:     src/components/auth/PublicRoute.tsx
```

## Documentation Files

- `AUTH_IMPLEMENTATION.md` - Technical implementation details
- `API_INTEGRATION.md` - Backend API requirements
- `AUTH_SETUP_COMPLETE.md` - Setup summary
- `QUICK_REFERENCE.md` - This file
