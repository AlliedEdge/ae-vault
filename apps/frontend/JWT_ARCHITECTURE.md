# JWT Authentication Architecture

Visual guide to understanding the JWT authentication flow in Ziboto.

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────────────┐   │
│  │  UI Layer    │  │  Auth Layer │  │   API Client Layer   │   │
│  ├──────────────┤  ├─────────────┤  ├──────────────────────┤   │
│  │ - Login Page │  │ AuthContext │  │  Axios Instance      │   │
│  │ - Protected  │  │ AuthStore   │  │  ├─ Request Int.     │   │
│  │   Routes     │  │ AuthService │  │  └─ Response Int.    │   │
│  │ - Dashboard  │  │             │  │                      │   │
│  └──────┬───────┘  └──────┬──────┘  └──────────┬───────────┘   │
│         │                 │                     │               │
│         └─────────────────┴─────────────────────┘               │
│                           │                                     │
│  ┌────────────────────────▼────────────────────────────────┐   │
│  │              Token Management Layer                      │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  TokenService          │  useTokenRefresh Hook           │   │
│  │  ├─ Access Token (mem) │  ├─ Session Restoration         │   │
│  │  ├─ Refresh Token (LS) │  ├─ Proactive Refresh           │   │
│  │  └─ Token Validation   │  └─ Cleanup on Logout           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
└───────────────────────────┬───────────────────────────────────┬─┘
                            │                                   │
                            ▼                                   │
                    ┌───────────────┐                          │
                    │  localStorage │                          │
                    ├───────────────┤                          │
                    │ Refresh Token │ ◄────────────────────────┘
                    │  (Encrypted)  │
                    └───────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │    Backend    │
                    │  Spring Boot  │
                    │ + JWT Security│
                    └───────────────┘
```

## Token Flow Diagram

### 1. Initial Login

```
┌──────┐                ┌──────────┐              ┌─────────┐
│ User │                │ Frontend │              │ Backend │
└──┬───┘                └────┬─────┘              └────┬────┘
   │                         │                         │
   │ Enter credentials       │                         │
   ├────────────────────────►│                         │
   │                         │                         │
   │                         │ POST /auth/login        │
   │                         │ {email, password}       │
   │                         ├────────────────────────►│
   │                         │                         │
   │                         │                    Validate
   │                         │                    credentials
   │                         │                         │
   │                         │  200 OK                 │
   │                         │  {accessToken,          │
   │                         │   refreshToken}         │
   │                         │◄────────────────────────┤
   │                         │                         │
   │                    Store tokens:                  │
   │                    • Access → memory              │
   │                    • Refresh → localStorage       │
   │                         │                         │
   │   Redirect to home      │                         │
   │◄────────────────────────┤                         │
   │                         │                         │
```

### 2. Authenticated API Request

```
┌──────────┐              ┌─────────┐
│ Frontend │              │ Backend │
└────┬─────┘              └────┬────┘
     │                         │
     │ GET /api/resource       │
     │ Authorization:          │
     │   Bearer <access-token> │
     ├────────────────────────►│
     │                         │
     │                    Validate JWT
     │                         │
     │ 200 OK + Data           │
     │◄────────────────────────┤
     │                         │
```

### 3. Token Refresh Flow

```
┌──────────┐              ┌─────────┐
│ Frontend │              │ Backend │
└────┬─────┘              └────┬────┘
     │                         │
     │ GET /api/resource       │
     │ (expired token)         │
     ├────────────────────────►│
     │                         │
     │ 401 Unauthorized        │
     │◄────────────────────────┤
     │                         │
Axios interceptor              │
catches 401                    │
     │                         │
     │ POST /auth/refresh      │
     │ {refreshToken}          │
     ├────────────────────────►│
     │                         │
     │                    Validate
     │                    refresh token
     │                         │
     │ 200 OK                  │
     │ {accessToken,           │
     │  refreshToken}          │
     │◄────────────────────────┤
     │                         │
Update access token            │
in memory                      │
     │                         │
     │ Retry original request  │
     │ GET /api/resource       │
     │ (new token)             │
     ├────────────────────────►│
     │                         │
     │ 200 OK + Data           │
     │◄────────────────────────┤
     │                         │
```

### 4. Concurrent Requests During Refresh

```
┌──────────┐              ┌─────────┐
│ Frontend │              │ Backend │
└────┬─────┘              └────┬────┘
     │                         │
     │ Request A (401)         │
     ├────────────────────────►│
     │                         │
     │ Request B (401)         │
     ├─────────────────────────┤
     │                         │
     │ Request C (401)         │
     ├─────────────────────────┤
     │                         │
     │                         │
Start refresh                  │
(isRefreshing = true)          │
Queue B and C                  │
     │                         │
     │ POST /auth/refresh      │
     ├────────────────────────►│
     │                         │
     │ New tokens              │
     │◄────────────────────────┤
     │                         │
Update token                   │
(isRefreshing = false)         │
     │                         │
Process queue:                 │
• Retry A with new token       │
• Retry B with new token       │
• Retry C with new token       │
     │                         │
```

### 5. Session Restoration (Page Refresh)

```
┌──────┐         ┌──────────┐              ┌─────────┐
│ User │         │ Frontend │              │ Backend │
└──┬───┘         └────┬─────┘              └────┬────┘
   │                  │                         │
   │ Refresh page     │                         │
   ├─────────────────►│                         │
   │                  │                         │
   │             App loads                      │
   │             Access token lost              │
   │             (was in memory)                │
   │                  │                         │
   │          useTokenRefresh                   │
   │          checks storage                    │
   │                  │                         │
   │          Found refresh token               │
   │          in localStorage                   │
   │                  │                         │
   │                  │ POST /auth/refresh      │
   │                  │ {refreshToken}          │
   │                  ├────────────────────────►│
   │                  │                         │
   │                  │ New access token        │
   │                  │◄────────────────────────┤
   │                  │                         │
   │          Store access token                │
   │          in memory                         │
   │                  │                         │
   │          Session restored                  │
   │                  │                         │
   │   User still logged in                     │
   │◄─────────────────┤                         │
   │                  │                         │
```

### 6. Logout Flow

```
┌──────┐         ┌──────────┐              ┌─────────┐
│ User │         │ Frontend │              │ Backend │
└──┬───┘         └────┬─────┘              └────┬────┘
   │                  │                         │
   │ Click logout     │                         │
   ├─────────────────►│                         │
   │                  │                         │
   │                  │ POST /auth/logout       │
   │                  ├────────────────────────►│
   │                  │                         │
   │                  │ 200 OK                  │
   │                  │◄────────────────────────┤
   │                  │                         │
   │          Clear tokens:                     │
   │          • memory (access)                 │
   │          • localStorage (refresh)          │
   │                  │                         │
   │          Trigger storage event             │
   │          (for multi-tab sync)              │
   │                  │                         │
   │   Redirect to login                        │
   │◄─────────────────┤                         │
   │                  │                         │
```

### 7. Token Expiry Handling

```
┌──────────────────────────────────────────┐
│           Token Lifecycle                │
└──────────────────────────────────────────┘

Time: 0 min
┌─────────────────────────────────────────┐
│ Login successful                         │
│ • Access Token: Valid (15 min TTL)      │
│ • Refresh Token: Valid (7 days TTL)     │
└─────────────────────────────────────────┘

Time: 13 min (2 min before expiry)
┌─────────────────────────────────────────┐
│ Proactive Refresh Triggered              │
│ • Old access token still valid           │
│ • useTokenRefresh timer fires            │
│ • POST /auth/refresh                     │
│ • New access token received              │
│ • Old token discarded                    │
└─────────────────────────────────────────┘

Time: 15 min (if proactive refresh failed)
┌─────────────────────────────────────────┐
│ Access Token Expired                     │
│ • User makes API request                 │
│ • 401 Unauthorized received              │
│ • Axios interceptor catches error        │
│ • Automatic refresh triggered            │
│ • Request retried with new token         │
└─────────────────────────────────────────┘

Time: 7 days
┌─────────────────────────────────────────┐
│ Refresh Token Expired                    │
│ • Refresh attempt fails (401)            │
│ • All tokens cleared                     │
│ • User logged out automatically          │
│ • Redirect to login page                 │
└─────────────────────────────────────────┘
```

## Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                              User Action                             │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                            UI Components                             │
│  (LoginPage, Dashboard, ProtectedRoutes)                            │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           Auth Context                               │
│  • Manages auth state                                               │
│  • Listens for token refresh failures                               │
│  • Handles multi-tab synchronization                                │
│  • Triggers logout on expiry                                        │
└────────────┬───────────────────────┬────────────────────────────────┘
             │                       │
             ▼                       ▼
┌────────────────────┐  ┌─────────────────────────────────┐
│    Auth Store      │  │    useTokenRefresh Hook         │
│  (Zustand)         │  │                                 │
│  • User data       │  │  • Session restoration          │
│  • Loading states  │  │  • Proactive refresh            │
│  • Error handling  │  │  • Timer management             │
└────────┬───────────┘  └────────────┬────────────────────┘
         │                           │
         └───────────┬───────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          Auth Service                                │
│  • login()    • register()    • logout()                            │
│  • refreshToken()    • getProfile()                                 │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Axios Instance                                │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  Request Interceptor                                        │    │
│  │  • Get access token from TokenService                       │    │
│  │  • Attach as Bearer token to all requests                   │    │
│  └────────────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  Response Interceptor                                       │    │
│  │  • Catch 401 errors                                         │    │
│  │  • Trigger token refresh                                    │    │
│  │  • Queue concurrent requests                                │    │
│  │  • Retry failed requests                                    │    │
│  │  • Handle refresh failures                                  │    │
│  └────────────────────────────────────────────────────────────┘    │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Token Service                                 │
│  ┌─────────────────────────┐  ┌──────────────────────────────┐    │
│  │   In-Memory Storage     │  │  localStorage (Encrypted)     │    │
│  │  ┌──────────────────┐   │  │  ┌────────────────────────┐  │    │
│  │  │  Access Token    │   │  │  │  Refresh Token         │  │    │
│  │  │  (JWT)           │   │  │  │  (Encrypted JWT)       │  │    │
│  │  │  TTL: 15-30 min  │   │  │  │  TTL: 7-30 days        │  │    │
│  │  └──────────────────┘   │  │  └────────────────────────┘  │    │
│  └─────────────────────────┘  └──────────────────────────────┘    │
│                                                                      │
│  Methods:                                                           │
│  • getAccessToken()    • getRefreshToken()                         │
│  • setTokens()         • setAccessToken()                          │
│  • clearTokens()       • isTokenExpired()                          │
│  • hasValidTokens()    • decodeToken()                             │
└─────────────────────────────────────────────────────────────────────┘
```

## Data Flow Summary

### Login → Store → Request → Logout

```
   Login
     │
     ▼
┌─────────┐
│ Backend │──► { accessToken, refreshToken }
└─────────┘
     │
     ▼
┌──────────────┐
│ TokenService │──► Store access token in memory
└──────────────┘──► Store refresh token in localStorage
     │
     ▼
┌──────────┐
│ API Call │──► Axios attaches Bearer token
└──────────┘
     │
     ▼
┌─────────┐
│ 200 OK  │──► Success
└─────────┘
     │
     ▼
┌─────────┐
│ 401 401 │──► Token expired
└─────────┘
     │
     ▼
┌─────────────┐
│ Auto Refresh│──► POST /auth/refresh
└─────────────┘
     │
     ▼
┌─────────────┐
│ New Token   │──► Update in memory
└─────────────┘
     │
     ▼
┌─────────────┐
│ Retry       │──► Original request succeeds
└─────────────┘
     │
     ▼
┌─────────────┐
│ User Action │──► Logout
└─────────────┘
     │
     ▼
┌──────────────┐
│ Clear Tokens │──► Memory + localStorage cleared
└──────────────┘
     │
     ▼
┌──────────────┐
│ Redirect     │──► Login page
└──────────────┘
```

## Security Layers

```
┌────────────────────────────────────────────────────────────┐
│                    Security Layers                          │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  Layer 1: Transport Security                               │
│  ├─ HTTPS/TLS encryption                                   │
│  └─ Secure headers (CSP, HSTS)                             │
│                                                             │
│  Layer 2: Token Storage                                    │
│  ├─ Access token in memory (XSS protected)                 │
│  ├─ Refresh token encrypted in localStorage                │
│  └─ Consider httpOnly cookies for production               │
│                                                             │
│  Layer 3: Token Management                                 │
│  ├─ Short-lived access tokens (15-30 min)                  │
│  ├─ Long-lived refresh tokens (7-30 days)                  │
│  └─ Automatic token rotation (recommended)                 │
│                                                             │
│  Layer 4: Request Security                                 │
│  ├─ Bearer token authentication                            │
│  ├─ Token validation on backend                            │
│  └─ Protection against replay attacks                      │
│                                                             │
│  Layer 5: Session Management                               │
│  ├─ Automatic logout on token expiry                       │
│  ├─ Multi-tab synchronization                              │
│  └─ Proper cleanup on logout                               │
│                                                             │
│  Layer 6: Error Handling                                   │
│  ├─ Infinite loop prevention                               │
│  ├─ Race condition handling                                │
│  └─ Graceful failure recovery                              │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

## Performance Optimizations

```
┌────────────────────────────────────────────────────────────┐
│              Performance Optimizations                      │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Request Queuing                                        │
│     └─ Concurrent requests queued during token refresh     │
│        ✓ Prevents multiple refresh attempts                │
│        ✓ Reduces backend load                              │
│                                                             │
│  2. Proactive Token Refresh                                │
│     └─ Refresh before expiry (2 min buffer)                │
│        ✓ Prevents 401 errors during active sessions        │
│        ✓ Better user experience (no interruption)          │
│                                                             │
│  3. Memory Storage for Access Tokens                       │
│     └─ Fast access without localStorage overhead           │
│        ✓ No serialization/deserialization cost             │
│        ✓ Faster request processing                         │
│                                                             │
│  4. Selective Token Refresh                                │
│     └─ Only refresh when needed                            │
│        ✓ Reduces unnecessary API calls                     │
│        ✓ Conserves bandwidth                               │
│                                                             │
│  5. Lazy Session Restoration                               │
│     └─ Restore session only if refresh token exists        │
│        ✓ Faster initial load for logged-out users          │
│        ✓ Seamless for logged-in users                      │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

---

**This architecture provides:**
- ✅ Secure token management
- ✅ Automatic token refresh
- ✅ Session persistence across page reloads
- ✅ Multi-tab synchronization
- ✅ Graceful error handling
- ✅ Performance optimizations
- ✅ Production-ready security
