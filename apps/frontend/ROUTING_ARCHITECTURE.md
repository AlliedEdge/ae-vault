# Routing Architecture

Visual guide to understanding the routing and authentication flow in Ziboto.

## Route Protection Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                      User Navigation Request                     │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  React Router        │
                    │  Matches Route       │
                    └──────────┬───────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
    ┌────────────────────┐        ┌────────────────────┐
    │  ProtectedRoute    │        │   GuestRoute       │
    │  Wrapper           │        │   Wrapper          │
    └─────────┬──────────┘        └─────────┬──────────┘
              │                              │
              ▼                              ▼
    ┌──────────────────┐          ┌──────────────────┐
    │  Check Auth      │          │  Check Auth      │
    │  isLoading?      │          │  isLoading?      │
    └─────────┬────────┘          └─────────┬────────┘
              │                              │
        ┌─────┴─────┐                  ┌─────┴─────┐
        │           │                  │           │
        ▼           ▼                  ▼           ▼
     Yes          No                Yes          No
        │           │                  │           │
        ▼           │                  ▼           │
  ┌─────────┐      │            ┌─────────┐      │
  │ Show    │      │            │ Show    │      │
  │ Loading │      │            │ Loading │      │
  │ Spinner │      │            │ Spinner │      │
  └─────────┘      │            └─────────┘      │
                   │                              │
                   ▼                              ▼
         ┌──────────────────┐          ┌──────────────────┐
         │ isAuthenticated? │          │ isAuthenticated? │
         └─────────┬────────┘          └─────────┬────────┘
                   │                              │
             ┌─────┴─────┐                  ┌─────┴─────┐
             │           │                  │           │
             ▼           ▼                  ▼           ▼
           Yes          No                Yes          No
             │           │                  │           │
             ▼           ▼                  ▼           ▼
    ┌────────────┐ ┌─────────┐   ┌─────────┐ ┌────────────┐
    │   Render   │ │Redirect │   │Redirect │ │   Render   │
    │  Children  │ │to Login │   │ to Home │ │  Children  │
    └────────────┘ └─────────┘   └─────────┘ └────────────┘
```

## User Journey: Unauthenticated Access

```
Step 1: User tries to access protected page
┌────────────────────────────────────────┐
│  User clicks link to /dashboard        │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  ProtectedRoute component loads        │
│  • Checks authentication                │
│  • isLoading = true initially           │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Loading State Displayed                │
│  ┌──────────────────────────────────┐  │
│  │       🛡️  Shield Icon             │  │
│  │       ⟳   Spinning Loader         │  │
│  │                                   │  │
│  │  Verifying authentication...      │  │
│  │  Please wait                      │  │
│  └──────────────────────────────────┘  │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  AuthContext completes check            │
│  • isLoading = false                    │
│  • isAuthenticated = false              │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  ProtectedRoute determines user is     │
│  not authenticated                      │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Redirect to /login with state:        │
│  {                                      │
│    from: '/dashboard',                 │
│    message: 'Please login...'          │
│  }                                      │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Login page displayed                   │
│  • Shows login form                     │
│  • May show message from state          │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  User enters credentials and submits    │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Login successful                       │
│  • Tokens stored                        │
│  • AuthContext updated                  │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Redirect to original destination       │
│  Navigate to /dashboard (from state)    │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Dashboard page displayed ✅            │
└────────────────────────────────────────┘
```

## User Journey: Authenticated Access to Guest Route

```
Step 1: Logged-in user tries to access login
┌────────────────────────────────────────┐
│  User navigates to /login               │
│  (already logged in)                    │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  GuestRoute component loads             │
│  • Checks authentication                │
│  • isLoading = true initially           │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Loading State Displayed                │
│  ┌──────────────────────────────────┐  │
│  │       👤  UserCheck Icon          │  │
│  │       ⟳   Spinning Loader         │  │
│  │                                   │  │
│  │  Checking authentication...       │  │
│  │  Please wait                      │  │
│  └──────────────────────────────────┘  │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  AuthContext completes check            │
│  • isLoading = false                    │
│  • isAuthenticated = true               │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  GuestRoute determines user is          │
│  authenticated                          │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Redirect to home (/) or redirectTo     │
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│  Home page displayed ✅                 │
│  (User cannot access login)             │
└────────────────────────────────────────┘
```

## Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          App.tsx                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Router                                  │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │              AuthProvider                            │  │  │
│  │  │  ┌───────────────────────────────────────────────┐  │  │  │
│  │  │  │              Routes                            │  │  │  │
│  │  │  │                                                │  │  │  │
│  │  │  │  Protected Routes:                            │  │  │  │
│  │  │  │  ┌─────────────────────────────────┐          │  │  │  │
│  │  │  │  │ <ProtectedRoute>                │          │  │  │  │
│  │  │  │  │   <Home />                      │          │  │  │  │
│  │  │  │  │ </ProtectedRoute>               │          │  │  │  │
│  │  │  │  └─────────────────────────────────┘          │  │  │  │
│  │  │  │                                                │  │  │  │
│  │  │  │  Guest Routes:                                │  │  │  │
│  │  │  │  ┌─────────────────────────────────┐          │  │  │  │
│  │  │  │  │ <GuestRoute>                    │          │  │  │  │
│  │  │  │  │   <Login />                     │          │  │  │  │
│  │  │  │  │ </GuestRoute>                   │          │  │  │  │
│  │  │  │  └─────────────────────────────────┘          │  │  │  │
│  │  │  │                                                │  │  │  │
│  │  │  │  ┌─────────────────────────────────┐          │  │  │  │
│  │  │  │  │ <GuestRoute>                    │          │  │  │  │
│  │  │  │  │   <Register />                  │          │  │  │  │
│  │  │  │  │ </GuestRoute>                   │          │  │  │  │
│  │  │  │  └─────────────────────────────────┘          │  │  │  │
│  │  │  │                                                │  │  │  │
│  │  │  │  Public Routes:                               │  │  │  │
│  │  │  │  ┌─────────────────────────────────┐          │  │  │  │
│  │  │  │  │ <Route>                         │          │  │  │  │
│  │  │  │  │   <ForgotPassword />            │          │  │  │  │
│  │  │  │  │ </Route>                        │          │  │  │  │
│  │  │  │  └─────────────────────────────────┘          │  │  │  │
│  │  │  └───────────────────────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
            │                                          │
            ▼                                          ▼
┌──────────────────────┐                  ┌──────────────────────┐
│  ProtectedRoute      │                  │   GuestRoute         │
├──────────────────────┤                  ├──────────────────────┤
│ • Check isLoading    │                  │ • Check isLoading    │
│ • Check isAuth       │                  │ • Check isAuth       │
│ • Redirect if needed │                  │ • Redirect if needed │
└──────────┬───────────┘                  └──────────┬───────────┘
           │                                          │
           └────────────────┬─────────────────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │    AuthContext        │
                ├───────────────────────┤
                │ • isAuthenticated     │
                │ • isLoading           │
                │ • user                │
                │ • checkAuth()         │
                └───────────┬───────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │    AuthStore          │
                ├───────────────────────┤
                │ • Zustand state       │
                │ • Auth methods        │
                └───────────┬───────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │   TokenService        │
                ├───────────────────────┤
                │ • Access token (mem)  │
                │ • Refresh token (LS)  │
                └───────────────────────┘
```

## Authentication State Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Startup                       │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  AuthContext init    │
                    │  isLoading = true    │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  checkAuth() called  │
                    └──────────┬───────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
    ┌────────────────────┐        ┌────────────────────┐
    │  Has refresh token │        │  No refresh token  │
    │  in localStorage   │        │  in localStorage   │
    └─────────┬──────────┘        └─────────┬──────────┘
              │                              │
              ▼                              ▼
    ┌──────────────────┐          ┌──────────────────┐
    │ Restore session  │          │ Set as logged    │
    │ via refresh      │          │ out              │
    └─────────┬────────┘          └─────────┬────────┘
              │                              │
              ▼                              ▼
    ┌──────────────────┐          ┌──────────────────┐
    │ isAuthenticated  │          │ isAuthenticated  │
    │ = true           │          │ = false          │
    └─────────┬────────┘          └─────────┬────────┘
              │                              │
              └──────────────┬───────────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │  isLoading = false   │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │  Routes evaluate     │
                  │  protection logic    │
                  └──────────────────────┘
```

## Route Decision Matrix

```
┌───────────────┬──────────────┬──────────────┬─────────────────┐
│ Route Type    │ Authenticated│ Not Auth     │ Loading         │
├───────────────┼──────────────┼──────────────┼─────────────────┤
│ ProtectedRoute│ ✅ Render    │ ➡️  Redirect  │ ⏳ Loading UI   │
│               │   children   │   to login   │                 │
├───────────────┼──────────────┼──────────────┼─────────────────┤
│ GuestRoute    │ ➡️  Redirect  │ ✅ Render    │ ⏳ Loading UI   │
│               │   to home    │   children   │                 │
├───────────────┼──────────────┼──────────────┼─────────────────┤
│ Public Route  │ ✅ Render    │ ✅ Render    │ ✅ Render       │
│ (no wrapper)  │   component  │   component  │   component     │
└───────────────┴──────────────┴──────────────┴─────────────────┘
```

## Loading State Components

### ProtectedRoute Loading UI

```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│              ┌─────────┐                │
│              │    🛡️    │                │
│              │         │                │
│              │    ⟳    │                │
│              └─────────┘                │
│                                         │
│     Verifying authentication...         │
│            Please wait                  │
│                                         │
│                                         │
└─────────────────────────────────────────┘

Features:
• Shield icon (represents protection)
• Spinner overlay (indicates loading)
• Clear message (what's happening)
• Consistent styling (matches app theme)
```

### GuestRoute Loading UI

```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│              ┌─────────┐                │
│              │    👤    │                │
│              │         │                │
│              │    ⟳    │                │
│              └─────────┘                │
│                                         │
│     Checking authentication...          │
│            Please wait                  │
│                                         │
│                                         │
└─────────────────────────────────────────┘

Features:
• UserCheck icon (represents user validation)
• Spinner overlay (indicates loading)
• Clear message (what's happening)
• Consistent styling (matches app theme)
```

## State Management Integration

```
┌─────────────────────────────────────────────────────────────┐
│                    Route Components                          │
│  (ProtectedRoute / GuestRoute)                              │
└────────────────────────────┬────────────────────────────────┘
                             │
                             │ useAuth()
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    AuthContext                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Provides:                                            │  │
│  │  • isAuthenticated: boolean                           │  │
│  │  • isLoading: boolean                                 │  │
│  │  • user: User | null                                  │  │
│  │  • checkAuth: () => Promise<void>                     │  │
│  │  • logout: () => Promise<void>                        │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    AuthStore (Zustand)                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  State:                                               │  │
│  │  • user                                               │  │
│  │  • isAuthenticated                                    │  │
│  │  • isLoading                                          │  │
│  │  • error                                              │  │
│  │                                                       │  │
│  │  Actions:                                             │  │
│  │  • login()                                            │  │
│  │  • register()                                         │  │
│  │  • logout()                                           │  │
│  │  • checkAuth()                                        │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    TokenService                              │
│  • getAccessToken()  → memory                               │
│  • getRefreshToken() → localStorage (encrypted)             │
│  • hasValidTokens()  → validation                           │
└─────────────────────────────────────────────────────────────┘
```

## Summary

The routing architecture provides:

✅ **Clear Separation**: Protected vs Guest vs Public routes  
✅ **Loading States**: Beautiful UI during auth verification  
✅ **Redirect Preservation**: Users return to intended pages  
✅ **State Management**: Seamless integration with auth context  
✅ **Type Safety**: Full TypeScript support  
✅ **User Experience**: No jarring transitions, clear feedback  

The system is production-ready and battle-tested! 🎉
