# Routing - Quick Reference

Quick guide for using route protection components in Ziboto.

## Components

| Component | Purpose | Redirects |
|-----------|---------|-----------|
| `ProtectedRoute` | For authenticated users only | Unauthenticated → `/login` |
| `GuestRoute` | For non-authenticated users only | Authenticated → `/` |

## Quick Examples

### Protect a Page (Requires Login)

```tsx
import { ProtectedRoute } from '@/components/auth';

<Route path="/dashboard" element={
  <ProtectedRoute>
    <Dashboard />
  </ProtectedRoute>
} />
```

### Guest-Only Page (Login/Register)

```tsx
import { GuestRoute } from '@/components/auth';

<Route path="/login" element={
  <GuestRoute>
    <Login />
  </GuestRoute>
} />
```

### Public Page (Accessible to All)

```tsx
// No wrapper needed
<Route path="/about" element={<About />} />
```

## Common Patterns

### Basic Setup

```tsx
import { ProtectedRoute, GuestRoute } from '@/components/auth';

<Routes>
  {/* Protected - requires auth */}
  <Route path="/" element={
    <ProtectedRoute>
      <Home />
    </ProtectedRoute>
  } />

  {/* Guest - logged out users only */}
  <Route path="/login" element={
    <GuestRoute>
      <Login />
    </GuestRoute>
  } />

  {/* Public - everyone */}
  <Route path="/about" element={<About />} />
</Routes>
```

### Custom Redirect Paths

```tsx
// Custom fallback for protected route
<ProtectedRoute fallbackPath="/unauthorized">
  <AdminPanel />
</ProtectedRoute>

// Custom redirect for guest route
<GuestRoute redirectTo="/dashboard">
  <Login />
</GuestRoute>
```

### Post-Login Redirect

```tsx
// In Login component
import { useLocation, useNavigate } from 'react-router-dom';

const Login = () => {
  const location = useLocation();
  const navigate = useNavigate();
  
  const from = location.state?.from || '/';
  
  const handleLogin = async (credentials) => {
    await login(credentials);
    navigate(from, { replace: true });
  };
  
  return <LoginForm onSubmit={handleLogin} />;
};
```

## User Flows

### Flow 1: Accessing Protected Page (Logged Out)

```
Visit /dashboard → Redirect to /login → Login → Back to /dashboard
```

### Flow 2: Accessing Guest Page (Logged In)

```
Visit /login → Redirect to / → Home page
```

### Flow 3: Direct Navigation (Logged In)

```
Visit /dashboard → Access granted → Dashboard page
```

## What Gets Displayed

### While Loading (Both Components)

```
┌─────────────────────────────────────┐
│                                     │
│        🛡️  (Shield Icon)            │
│        ⟳  (Spinning)                │
│                                     │
│   Verifying authentication...       │
│   Please wait                       │
│                                     │
└─────────────────────────────────────┘
```

### After Loaded

- **ProtectedRoute**: Shows children if authenticated, else redirects
- **GuestRoute**: Shows children if NOT authenticated, else redirects

## Props Reference

### ProtectedRoute

```tsx
interface ProtectedRouteProps {
  children: ReactNode;        // Required
  fallbackPath?: string;      // Default: '/login'
}
```

### GuestRoute

```tsx
interface GuestRouteProps {
  children: ReactNode;        // Required
  redirectTo?: string;        // Default: '/'
}
```

## Route Types

### ✅ Protected Routes (Use ProtectedRoute)
- Dashboard
- Profile
- Settings
- User-specific pages
- Any page requiring authentication

### 🚪 Guest Routes (Use GuestRoute)
- Login
- Register
- (Forgot Password - optional)

### 🌍 Public Routes (No wrapper)
- Landing page (if public)
- About page
- Contact page
- Terms of service
- Privacy policy
- Password reset page
- Email verification page

## Testing Checklist

```
ProtectedRoute:
[ ] Shows loading state
[ ] Redirects to login when not authenticated
[ ] Shows page when authenticated
[ ] Preserves intended destination

GuestRoute:
[ ] Shows loading state
[ ] Redirects to home when authenticated
[ ] Shows page when not authenticated
[ ] Cannot access login when logged in
```

## Common Mistakes

### ❌ Wrong

```tsx
// Don't use GuestRoute for protected pages
<Route path="/dashboard" element={
  <GuestRoute>
    <Dashboard />
  </GuestRoute>
} />

// Don't use ProtectedRoute for login
<Route path="/login" element={
  <ProtectedRoute>
    <Login />
  </ProtectedRoute>
} />

// Don't forget to wrap auth pages
<Route path="/login" element={<Login />} />
```

### ✅ Correct

```tsx
// Protected pages use ProtectedRoute
<Route path="/dashboard" element={
  <ProtectedRoute>
    <Dashboard />
  </ProtectedRoute>
} />

// Guest pages use GuestRoute
<Route path="/login" element={
  <GuestRoute>
    <Login />
  </GuestRoute>
} />
```

## Debug Tips

### Check Authentication Status

```javascript
// In browser console
window.jwtTest.getStatus()
```

### Check Current Route Protection

1. Open browser console
2. Try accessing protected route while logged out
3. Check if redirected to login
4. Try accessing login while logged in
5. Check if redirected to home

### Verify Loading State

1. Slow down network in DevTools (Slow 3G)
2. Navigate to protected/guest route
3. Should see loading spinner
4. Should see proper page after loading

## Quick Troubleshooting

| Issue | Solution |
|-------|----------|
| Stuck on loading | Check AuthContext `isLoading` |
| Redirect loop | Verify route wrappers are correct |
| Can't access page | Check authentication status |
| Post-login redirect broken | Verify `location.state.from` handling |

## Need More Details?

See [ROUTING.md](./ROUTING.md) for comprehensive documentation.

---

**Remember:**
- 🛡️ Protected = Authenticated users only
- 🚪 Guest = Non-authenticated users only
- 🌍 Public = Everyone
