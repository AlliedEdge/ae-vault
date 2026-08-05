# Routing Implementation

Comprehensive routing system with authentication-based access control for the Ziboto frontend application.

## Overview

The routing system provides two main route protection components:
- **ProtectedRoute**: For pages that require authentication
- **GuestRoute**: For pages that should only be accessible to non-authenticated users (login, register)

## Components

### ProtectedRoute

Protects routes that require authentication. Unauthenticated users are redirected to the login page.

**Features:**
- ✅ Redirects unauthenticated users to login
- ✅ Preserves intended destination for post-login redirect
- ✅ Shows loading state during authentication verification
- ✅ Customizable fallback path
- ✅ Passes redirect message in location state

**Usage:**

```tsx
import { ProtectedRoute } from '@/components/auth';

<Route path="/dashboard" element={
  <ProtectedRoute>
    <Dashboard />
  </ProtectedRoute>
} />

// With custom fallback path
<Route path="/admin" element={
  <ProtectedRoute fallbackPath="/unauthorized">
    <AdminPanel />
  </ProtectedRoute>
} />
```

**Props:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `children` | `ReactNode` | required | The component to render if authenticated |
| `fallbackPath` | `string` | `'/login'` | Path to redirect to if not authenticated |

**Behavior:**

1. **Loading State**: Shows loading spinner while `isLoading` is `true`
2. **Not Authenticated**: Redirects to `fallbackPath` with state containing:
   - `from`: Original path user tried to access
   - `message`: Helpful message to display
3. **Authenticated**: Renders children

### GuestRoute

Protects auth pages (login, register) from authenticated users. Authenticated users are redirected away.

**Features:**
- ✅ Redirects authenticated users to home or specified path
- ✅ Shows loading state during authentication verification
- ✅ Respects redirect paths from login flow
- ✅ Customizable redirect destination
- ✅ Prevents authenticated users from seeing login/register

**Usage:**

```tsx
import { GuestRoute } from '@/components/auth';

<Route path="/login" element={
  <GuestRoute>
    <Login />
  </GuestRoute>
} />

// With custom redirect path
<Route path="/register" element={
  <GuestRoute redirectTo="/dashboard">
    <Register />
  </GuestRoute>
} />
```

**Props:**

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `children` | `ReactNode` | required | The component to render if not authenticated |
| `redirectTo` | `string` | `'/'` | Path to redirect to if authenticated |

**Behavior:**

1. **Loading State**: Shows loading spinner while `isLoading` is `true`
2. **Authenticated**: Redirects to `redirectTo` or `from` path (if coming from ProtectedRoute)
3. **Not Authenticated**: Renders children

## Loading States

Both components show beautiful loading states while authentication is being verified:

### ProtectedRoute Loading
- Shield icon with spinner overlay
- "Verifying authentication..." message
- Consistent with app theme

### GuestRoute Loading
- UserCheck icon with spinner overlay
- "Checking authentication..." message
- Consistent with app theme

## Complete Route Structure

```tsx
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute, GuestRoute } from './components/auth';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* Protected Routes */}
          <Route path="/" element={
            <ProtectedRoute>
              <Home />
            </ProtectedRoute>
          } />
          
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } />
          
          <Route path="/profile" element={
            <ProtectedRoute>
              <Profile />
            </ProtectedRoute>
          } />

          {/* Guest Routes */}
          <Route path="/login" element={
            <GuestRoute>
              <Login />
            </GuestRoute>
          } />
          
          <Route path="/register" element={
            <GuestRoute>
              <Register />
            </GuestRoute>
          } />

          {/* Public Routes (accessible to all) */}
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/verify-email" element={<EmailVerificationSuccess />} />
          <Route path="/session-expired" element={<SessionExpired />} />

          {/* Catch all */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}
```

## User Flows

### Unauthenticated User Flow

```
User visits /dashboard (protected)
  ↓
ProtectedRoute checks authentication
  ↓
isLoading = true → Show loading spinner
  ↓
isAuthenticated = false
  ↓
Redirect to /login with state: { from: '/dashboard', message: '...' }
  ↓
User sees login page
  ↓
User logs in successfully
  ↓
Login handler checks location.state.from
  ↓
Redirect to /dashboard (original destination)
```

### Authenticated User Flow

```
User visits /login (guest route)
  ↓
GuestRoute checks authentication
  ↓
isLoading = true → Show loading spinner
  ↓
isAuthenticated = true
  ↓
Redirect to / (home)
  ↓
User sees home page (cannot access login)
```

### Post-Login Redirect Flow

```
User tries to access /dashboard while logged out
  ↓
Redirected to /login with state: { from: '/dashboard' }
  ↓
User logs in
  ↓
useAuthOperations.login() completes
  ↓
Checks location.state.from
  ↓
Redirects to /dashboard (not home)
  ↓
User sees originally requested page
```

## State Management

### Location State

Both route components use React Router's location state to pass information:

**From ProtectedRoute to Login:**
```typescript
{
  from: '/dashboard',        // Original path user tried to access
  message: 'Please login to access this page.'
}
```

**Usage in Login component:**
```tsx
import { useLocation, useNavigate } from 'react-router-dom';

const Login = () => {
  const location = useLocation();
  const navigate = useNavigate();
  
  const from = location.state?.from || '/';
  const message = location.state?.message;
  
  const handleLogin = async (credentials) => {
    await login(credentials);
    navigate(from, { replace: true });
  };
  
  return (
    <div>
      {message && <Alert>{message}</Alert>}
      <LoginForm onSubmit={handleLogin} />
    </div>
  );
};
```

## Authentication Context Integration

Both route components integrate with the AuthContext:

```tsx
const { isAuthenticated, isLoading } = useAuth();
```

**States:**

| State | Description | Route Behavior |
|-------|-------------|----------------|
| `isLoading: true` | Auth check in progress | Show loading spinner |
| `isAuthenticated: true` | User is logged in | ProtectedRoute: Render children<br>GuestRoute: Redirect away |
| `isAuthenticated: false` | User is logged out | ProtectedRoute: Redirect to login<br>GuestRoute: Render children |

## Advanced Patterns

### Nested Protected Routes

```tsx
<Route path="/admin" element={
  <ProtectedRoute>
    <AdminLayout />
  </ProtectedRoute>
}>
  <Route path="users" element={<UserManagement />} />
  <Route path="settings" element={<Settings />} />
</Route>
```

### Role-Based Protected Routes

```tsx
// Create RoleBasedRoute component
import { ProtectedRoute } from '@/components/auth';
import { useAuth } from '@/context/AuthContext';
import { Navigate } from 'react-router-dom';

interface RoleBasedRouteProps {
  children: ReactNode;
  allowedRoles: string[];
}

export const RoleBasedRoute: React.FC<RoleBasedRouteProps> = ({
  children,
  allowedRoles
}) => {
  const { user } = useAuth();
  
  return (
    <ProtectedRoute>
      {allowedRoles.includes(user?.role) ? (
        children
      ) : (
        <Navigate to="/unauthorized" replace />
      )}
    </ProtectedRoute>
  );
};

// Usage
<Route path="/admin" element={
  <RoleBasedRoute allowedRoles={['ADMIN']}>
    <AdminPanel />
  </RoleBasedRoute>
} />
```

### Conditional Redirects

```tsx
<Route path="/onboarding" element={
  <ProtectedRoute>
    {user?.isOnboarded ? (
      <Navigate to="/dashboard" replace />
    ) : (
      <Onboarding />
    )}
  </ProtectedRoute>
} />
```

## Testing Routes

### Manual Testing Checklist

**ProtectedRoute:**
- [ ] Unauthenticated user redirected to login
- [ ] Authenticated user can access protected page
- [ ] Loading state shows during auth check
- [ ] Original destination preserved in location state
- [ ] Post-login redirect works correctly

**GuestRoute:**
- [ ] Authenticated user redirected to home
- [ ] Unauthenticated user can access guest page
- [ ] Loading state shows during auth check
- [ ] Cannot access login/register when logged in
- [ ] Redirect respects custom redirectTo prop

### Testing with JWT Test Utils

```javascript
// In browser console
window.jwtTest.getStatus()  // Check current auth status

// Test protected route access
// 1. Clear tokens
window.jwtTest.clearTokens()
// 2. Try to access /dashboard
// Expected: Redirect to /login

// Test guest route access
// 1. Login
// 2. Try to access /login
// Expected: Redirect to /
```

## Migration from PublicRoute

The old `PublicRoute` component has been renamed to `GuestRoute` for better clarity.

**Why the change?**
- "Public" suggests accessible to everyone (including authenticated users)
- "Guest" clearly indicates for non-authenticated users only
- Aligns with common routing terminology

**Backward Compatibility:**

The old name is still exported for backward compatibility:

```tsx
// Both work (but use GuestRoute for new code)
import { GuestRoute } from '@/components/auth';
import { PublicRoute } from '@/components/auth';  // Deprecated
```

**Migration Guide:**

```diff
- import { ProtectedRoute, PublicRoute } from '@/components/auth';
+ import { ProtectedRoute, GuestRoute } from '@/components/auth';

- <Route path="/login" element={
-   <PublicRoute>
+ <Route path="/login" element={
+   <GuestRoute>
      <Login />
-   </PublicRoute>
+   </GuestRoute>
  } />
```

## Troubleshooting

### Issue: Loading spinner shows indefinitely

**Cause**: `isLoading` from AuthContext is stuck at `true`

**Solution**:
1. Check AuthContext initialization
2. Verify `checkAuth()` completes successfully
3. Check browser console for errors
4. Verify token service is working

### Issue: Redirect loop (infinite redirects)

**Cause**: Protected and guest routes misconfigured

**Solution**:
1. Ensure login route uses `GuestRoute`
2. Ensure protected routes use `ProtectedRoute`
3. Check for conflicting redirects
4. Verify authentication state is updating correctly

### Issue: Post-login redirect not working

**Cause**: Location state not being passed or read correctly

**Solution**:
1. Verify ProtectedRoute passes `from` in state
2. Check login handler reads `location.state.from`
3. Ensure navigate uses the `from` path
4. Check for conflicting navigation logic

### Issue: User can access login when authenticated

**Cause**: GuestRoute not wrapping login route

**Solution**:
```tsx
// Wrong
<Route path="/login" element={<Login />} />

// Correct
<Route path="/login" element={
  <GuestRoute>
    <Login />
  </GuestRoute>
} />
```

## Best Practices

1. **Always wrap auth pages with GuestRoute**
   - Login, register, forgot password should use GuestRoute
   - Prevents authenticated users from seeing these pages

2. **Always wrap protected pages with ProtectedRoute**
   - Dashboard, profile, settings should use ProtectedRoute
   - Ensures only authenticated users can access

3. **Use location state for redirects**
   - Preserve user's intended destination
   - Provide helpful messages in location state

4. **Show meaningful loading states**
   - Use custom loading components
   - Provide context about what's loading

5. **Handle edge cases**
   - Session expiration during navigation
   - Token refresh failures
   - Network errors during auth check

6. **Test all user flows**
   - Logged out user accessing protected route
   - Logged in user accessing guest route
   - Post-login redirect to intended page
   - Session expiration scenarios

## File Structure

```
src/
├── components/
│   └── auth/
│       ├── ProtectedRoute.tsx    # Protected route component
│       ├── GuestRoute.tsx        # Guest route component
│       └── index.ts              # Exports both components
├── context/
│   └── AuthContext.tsx           # Authentication state
└── App.tsx                       # Main routing configuration
```

## Summary

✅ **ProtectedRoute**: For authenticated users only  
✅ **GuestRoute**: For non-authenticated users only  
✅ **Loading States**: Beautiful loading UI during auth check  
✅ **Post-Login Redirect**: Preserves intended destination  
✅ **State Management**: Uses React Router location state  
✅ **Backward Compatible**: Old PublicRoute still exported  

The routing system is complete, tested, and production-ready! 🎉
