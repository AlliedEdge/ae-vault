# Production Backend Architecture Integration

## Overview

This document describes how the React frontend is designed to work with the production backend architecture featuring:
- **Nginx Load Balancer** for traffic distribution
- **Multiple Spring Boot Instances** for horizontal scaling
- **Spring Security** for authentication and authorization
- **Redis** for rate limiting and session management
- **PostgreSQL** for persistent data storage

## Backend Architecture Flow

```
React Frontend
      ↓
Nginx Load Balancer (Round-robin/Sticky sessions)
      ↓
Spring Boot Instance 1, 2, 3... (Stateless)
      ↓
Spring Security (JWT validation)
      ↓
Redis (Rate limiting, session tracking)
      ↓
PostgreSQL (User data, refresh tokens, audit logs)
```

## Authentication Flow

### 1. User Login

```
User Login Request
      ↓
Nginx → Spring Boot Instance (Any)
      ↓
AuthenticationController
      ↓
AuthenticationService
      ↓
Redis Rate Limiting Check (prevent brute force)
      ↓
Failed Login Count Check (Redis)
      ↓
PostgreSQL User Lookup
      ↓
BCrypt Password Verification
      ↓
Generate JWT Access Token (Stateless, no DB storage)
      ↓
Generate Refresh Token
      ↓
Store Session Metadata in Redis (user_id, device_info, last_activity)
      ↓
Store Refresh Token in PostgreSQL (with expiry, device info)
      ↓
Create Audit Log (PostgreSQL)
      ↓
Return { user, accessToken, refreshToken }
```

### 2. Authenticated Request

```
React → Request with Bearer Token
      ↓
Nginx → Any Spring Boot Instance
      ↓
Spring Security Filter Chain
      ↓
JWT Signature Validation (no DB call)
      ↓
JWT Expiry Check (no DB call)
      ↓
Extract User from JWT Claims
      ↓
Check Redis for Revoked Tokens (optional, fast)
      ↓
Process Request
```

### 3. Token Refresh

```
Access Token Expired (401)
      ↓
Frontend Axios Interceptor Catches 401
      ↓
POST /auth/refresh with refreshToken
      ↓
Nginx → Any Spring Boot Instance
      ↓
Validate Refresh Token in PostgreSQL
      ↓
Check if Token Revoked/Expired
      ↓
Update Session Activity in Redis
      ↓
Generate New Access Token
      ↓
Optionally Rotate Refresh Token
      ↓
Return { accessToken, refreshToken }
      ↓
Frontend Updates Stored Tokens
      ↓
Retry Original Request with New Token
```

## Frontend Design Principles for Stateless Backend

### ✅ Current Implementation (Correct)

1. **No Session Assumption**: Frontend doesn't assume server-side sessions exist
2. **JWT-Based Authentication**: All authentication uses JWT tokens
3. **Stateless Requests**: Every request is self-contained with Bearer token
4. **Client-Side Token Storage**: Tokens stored in localStorage (not cookies)
5. **Automatic Token Refresh**: Axios interceptor handles token refresh transparently
6. **No Server Session Dependency**: Works with any backend instance via load balancer

### ✅ Stateless-Compatible Features

#### Token Management (`src/services/tokenService.ts`)
```typescript
// Tokens stored CLIENT-SIDE only
localStorage.setItem('ziboto_access_token', accessToken);
localStorage.setItem('ziboto_refresh_token', refreshToken);
localStorage.setItem('ziboto_token_expiry', expiry);

// No assumption of server-side session
// Token is the ONLY authentication mechanism
```

#### Axios Interceptor (`src/lib/axios.ts`)
```typescript
// Automatically adds token to EVERY request
config.headers.Authorization = `Bearer ${token}`;

// Works with ANY backend instance (stateless)
// No session cookies, no sticky sessions required
```

#### Token Refresh Logic
```typescript
// On 401 error:
// 1. Call /auth/refresh with refreshToken
// 2. Get new accessToken
// 3. Update localStorage
// 4. Retry original request
// 5. Works with any backend instance handling the retry
```

### ✅ Load Balancer Compatibility

#### Round-Robin Load Balancing Support
Your frontend supports round-robin load balancing because:

1. **Stateless Requests**: Each request includes full authentication context (JWT)
2. **No Session Affinity Required**: Any backend instance can validate any JWT
3. **Refresh Token in Request Body**: Refresh tokens sent in POST body, not cookies
4. **No Hidden State**: All state in JWT claims or explicit request parameters

#### Sticky Sessions NOT Required
Unlike traditional session-based auth, your frontend works WITHOUT sticky sessions:

```nginx
# This configuration is OPTIONAL for your architecture
upstream backend {
    # Round-robin works fine
    server backend1:8080;
    server backend2:8080;
    server backend3:8080;
    
    # Sticky sessions NOT required for JWT auth
    # ip_hash;  ← NOT needed
}
```

### ✅ Redis Integration Awareness

Your frontend is designed to work with Redis-backed features:

#### 1. Rate Limiting (Redis)
```typescript
// Frontend handles rate limit errors gracefully
if (error.response?.status === 429) {
  // "Too many requests. Please try again later."
  // Automatic retry with exponential backoff
}
```

#### 2. Token Revocation (Redis Blacklist)
```typescript
// If backend revokes token via Redis:
// - User logs out on Device A
// - Backend adds token to Redis blacklist
// - Request from Device B → 401
// - Frontend auto-refreshes or redirects to login
```

#### 3. Session Tracking (Redis)
```typescript
// Backend can track:
// - Active sessions (user_id → session_metadata)
// - Last activity timestamps
// - Device/browser fingerprints
// Frontend doesn't need to know about this
```

## Environment Configuration

### Frontend (.env)
```env
# Points to Nginx load balancer, not individual instances
VITE_API_URL=https://api.ziboto.com/api/v1

# Or for development (load balancer on localhost)
VITE_API_URL=http://localhost:8080/api/v1
```

### Nginx Configuration (Production)
```nginx
upstream ziboto_backend {
    # Multiple Spring Boot instances
    server backend1:8080 max_fails=3 fail_timeout=30s;
    server backend2:8080 max_fails=3 fail_timeout=30s;
    server backend3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.ziboto.com;

    # CORS headers for React frontend
    add_header 'Access-Control-Allow-Origin' 'https://ziboto.com' always;
    add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
    add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
    add_header 'Access-Control-Max-Age' 1728000 always;

    # Handle OPTIONS preflight
    if ($request_method = 'OPTIONS') {
        return 204;
    }

    location /api/v1/ {
        proxy_pass http://ziboto_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
        
        # Health checks
        proxy_next_upstream error timeout http_502 http_503 http_504;
    }
}
```

## Security Considerations

### 1. JWT Design (Backend Responsibility)

#### Access Token (Short-lived, Stateless)
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "role": "USER",
  "authorities": ["READ", "WRITE"],
  "iat": 1704067200,
  "exp": 1704068100  // 15 minutes
}
```

**Characteristics:**
- ✅ Stateless (no DB lookup on every request)
- ✅ Contains user info (no DB join needed)
- ✅ Short expiry (15 minutes) limits exposure
- ✅ Validated by signature only (fast)

#### Refresh Token (Long-lived, Stored in DB)
```json
{
  "sub": "user-uuid",
  "type": "REFRESH",
  "jti": "unique-token-id",  // Enables revocation
  "iat": 1704067200,
  "exp": 1704672000  // 7 days
}
```

**Characteristics:**
- ✅ Stored in PostgreSQL (can be revoked)
- ✅ Includes device/browser fingerprint
- ✅ One-time use (rotation on refresh)
- ✅ Can be invalidated on logout

### 2. Token Storage (Frontend)

#### ✅ Current: localStorage
```typescript
// Pros: Works across tabs, survives page refresh
// Cons: Vulnerable to XSS
localStorage.setItem('ziboto_access_token', token);
```

#### Alternative: sessionStorage
```typescript
// Pros: Cleared on tab close, slightly more secure
// Cons: Doesn't persist across tabs
sessionStorage.setItem('ziboto_access_token', token);
```

#### Alternative: httpOnly Cookies (Requires Backend Changes)
```typescript
// Pros: Not accessible to JavaScript (XSS protection)
// Cons: Requires backend to set cookies, CSRF protection needed
// Would require changing from localStorage to cookie-based auth
```

**Recommendation for Production:**
Keep localStorage but implement:
1. Content Security Policy (CSP) to prevent XSS
2. Strict input validation
3. Short access token expiry (15 minutes)
4. Token rotation on refresh

### 3. Rate Limiting (Redis-backed)

Frontend handles rate-limited responses:

```typescript
// In src/utils/retryHandler.ts
const AUTH_RETRY_CONFIG = {
  retryableStatusCodes: [408, 429, 500, 502, 503, 504],
  // 429 = Rate limited by Redis
  // Frontend waits and retries with exponential backoff
};
```

**Backend Rate Limits (Redis):**
- Login: 5 attempts per 15 minutes per IP
- Register: 3 attempts per hour per IP
- Password Reset: 3 requests per hour per IP
- Token Refresh: 10 requests per minute per user

### 4. Token Revocation Strategies

#### Logout (Active Revocation)
```
User clicks Logout
      ↓
POST /auth/logout with accessToken
      ↓
Backend adds token to Redis blacklist
      ↓
Backend deletes refresh token from PostgreSQL
      ↓
Backend deletes session from Redis
      ↓
Frontend clears localStorage
```

#### Forced Logout (Security Event)
```
Backend detects suspicious activity
      ↓
Revoke all refresh tokens in PostgreSQL
      ↓
Add all active access tokens to Redis blacklist
      ↓
User's next request → 401
      ↓
Frontend auto-redirects to login
```

## Frontend-Backend Contract

### Expected Response Formats

#### Success: Login/Register
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "John Doe",
    "role": "USER",
    "emailVerified": true
  },
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 900  // 15 minutes in seconds
}
```

#### Success: Token Refresh
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",  // May be same or rotated
  "expiresIn": 900
}
```

#### Error: Rate Limited (Redis)
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 5 minutes.",
  "path": "/api/v1/auth/login"
}
```

#### Error: Invalid Token
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/v1/users/me"
}
```

## Deployment Architecture

### Development
```
React (Vite Dev Server) :5173
      ↓
Spring Boot (Single Instance) :8080
      ↓
Redis :6379
      ↓
PostgreSQL :5432
```

### Production
```
React (Nginx Static) :443
      ↓
Nginx Load Balancer :443
      ↓
Spring Boot Instance 1 :8080
Spring Boot Instance 2 :8080
Spring Boot Instance 3 :8080
      ↓
Redis Cluster (Master-Replica)
      ↓
PostgreSQL (Primary + Read Replicas)
```

## Monitoring & Observability

### Frontend Metrics (Should Track)
1. Token refresh success rate
2. 401 error frequency
3. Average token lifetime before refresh
4. Login/logout success rates
5. API response times through load balancer

### Backend Metrics (Should Track)
1. JWT validation time per request
2. Redis cache hit rate
3. PostgreSQL query performance
4. Token refresh rate per user
5. Failed login attempts (rate limiting triggers)

### Logs to Implement

#### Frontend
```typescript
// On token refresh
console.log('[Auth] Token refreshed', {
  userId: user.id,
  expiresIn: response.expiresIn,
  timestamp: new Date().toISOString()
});

// On 401 error
console.error('[Auth] Unauthorized request', {
  url: request.url,
  hasToken: !!token,
  tokenExpired: tokenService.isTokenExpired()
});
```

#### Backend (Spring Boot)
```java
// On JWT validation
logger.info("JWT validated for user: {} from IP: {}", 
    userId, request.getRemoteAddr());

// On rate limit hit
logger.warn("Rate limit exceeded for IP: {} on endpoint: {}", 
    ip, endpoint);

// On token refresh
logger.info("Token refreshed for user: {} from device: {}", 
    userId, deviceInfo);
```

## Testing Strategies

### 1. Load Balancer Testing
```bash
# Test that auth works across different backend instances
for i in {1..100}; do
  curl -H "Authorization: Bearer $TOKEN" \
       https://api.ziboto.com/api/v1/users/me
done

# Should work regardless of which instance handles request
```

### 2. Token Refresh Under Load
```bash
# Simulate concurrent requests during token expiry
ab -n 1000 -c 10 -H "Authorization: Bearer $EXPIRED_TOKEN" \
   https://api.ziboto.com/api/v1/users/me

# Should see automatic refresh and retry
```

### 3. Redis Failure Simulation
```bash
# Stop Redis and verify graceful degradation
docker stop redis

# Auth should still work (JWT validation doesn't need Redis)
# Rate limiting might be bypassed (acceptable fallback)
```

### 4. PostgreSQL Failover
```bash
# Trigger PostgreSQL failover to replica
# Token refresh might fail temporarily
# Frontend should retry with exponential backoff
```

## Migration Checklist

If moving from session-based to JWT-based auth:

- [x] ✅ Remove session cookies from frontend
- [x] ✅ Implement token storage in localStorage
- [x] ✅ Add Authorization header to all requests
- [x] ✅ Implement token refresh interceptor
- [x] ✅ Remove JSESSIONID or similar session IDs
- [x] ✅ Test across multiple backend instances
- [ ] ⚠️ Implement token revocation (Redis blacklist)
- [ ] ⚠️ Add audit logging for security events
- [ ] ⚠️ Implement rate limiting (Redis)
- [ ] ⚠️ Add monitoring for token lifecycle
- [ ] ⚠️ Test load balancer health checks
- [ ] ⚠️ Document token expiry policies

## Troubleshooting Guide

### Issue: Token works on Instance 1, fails on Instance 2
**Cause:** Different JWT secret keys on instances
**Solution:** Ensure all instances share same JWT_SECRET environment variable

### Issue: Token refresh creates infinite loop
**Cause:** Refresh endpoint returns 401
**Solution:** Axios interceptor should NOT retry /auth/refresh endpoint (already implemented)

### Issue: Rate limiting inconsistent across instances
**Cause:** Each instance has local rate limiter
**Solution:** Use shared Redis for rate limiting (per your architecture)

### Issue: User logged out unexpectedly
**Causes:**
1. Token revoked in Redis blacklist
2. Refresh token deleted from PostgreSQL
3. Access token expired and refresh failed
**Solution:** Check backend logs for revocation events

### Issue: CORS errors in production
**Cause:** Nginx not configured for CORS
**Solution:** Add CORS headers to Nginx config (see above)

## Recommendations

### ✅ Already Implemented Correctly
1. Stateless JWT authentication
2. Client-side token storage
3. Automatic token refresh
4. Bearer token in headers
5. No session dependency
6. Load balancer compatible

### 🔧 Should Implement (Backend)
1. **Redis Token Blacklist**: For immediate logout
2. **Rate Limiting**: Using Redis counters
3. **Refresh Token Rotation**: One-time use tokens
4. **Device Tracking**: Store device info with refresh tokens
5. **Audit Logging**: All auth events to PostgreSQL

### 🔧 Should Implement (Frontend)
1. **Token Preemptive Refresh**: Refresh 5 minutes before expiry
2. **Device Fingerprinting**: Send device info on login/refresh
3. **Logout All Devices**: UI option to revoke all sessions
4. **Security Events UI**: Show "New device login detected"

### 🔒 Security Hardening
1. **CSP Headers**: Prevent XSS attacks
2. **HTTPS Only**: Enforce secure connections
3. **Token Binding**: Bind tokens to IP or device
4. **Brute Force Protection**: Exponential backoff on failed logins

## Conclusion

Your frontend is **correctly designed** for a stateless JWT backend architecture with:
- ✅ No server-side session assumptions
- ✅ Load balancer compatibility (round-robin works)
- ✅ Automatic token refresh
- ✅ Redis/PostgreSQL awareness
- ✅ Horizontal scaling support

The architecture supports multiple Spring Boot instances behind Nginx with JWT-based authentication, Redis for fast operations (rate limiting, token blacklist), and PostgreSQL for persistent data (users, refresh tokens, audit logs).

No major changes needed to the frontend implementation. Focus on implementing the backend components (Redis rate limiting, token blacklist, refresh token storage in PostgreSQL, audit logging).
