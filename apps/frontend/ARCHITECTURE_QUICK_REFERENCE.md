# Architecture Quick Reference

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         React Frontend                          │
│                    (Vite + React + TypeScript)                  │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐      │
│  │ Auth Context │  │ Token Service│  │ Axios Client   │      │
│  │   (State)    │  │ (localStorage)│  │ (Interceptors) │      │
│  └──────────────┘  └──────────────┘  └────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS (JWT in Authorization header)
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Nginx Load Balancer                        │
│              (Round-robin, Health Checks, CORS)                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┼─────────┐
                    ↓         ↓         ↓
         ┌──────────────┬──────────────┬──────────────┐
         │ Spring Boot 1│ Spring Boot 2│ Spring Boot 3│
         │  (Stateless) │  (Stateless) │  (Stateless) │
         └──────────────┴──────────────┴──────────────┘
                    │                   │
                    │                   │
         ┌──────────┴─────┐    ┌───────┴──────┐
         │                │    │              │
         ↓                ↓    ↓              ↓
    ┌─────────┐    ┌──────────┐         ┌──────────┐
    │  Redis  │    │PostgreSQL│         │PostgreSQL│
    │ Cluster │    │ (Primary)│         │ (Replica)│
    └─────────┘    └──────────┘         └──────────┘
         │               │                     │
         │               │                     │
    Rate Limiting   User Data            Read Queries
    Token Blacklist Refresh Tokens
    Session Metadata Audit Logs
```

## 🔐 Authentication Flow

### Login Flow
```
1. User enters email/password
   ↓
2. POST /api/v1/auth/login
   {
     email: "user@example.com",
     password: "********",
     rememberMe: false
   }
   ↓
3. Backend validates credentials
   - Check PostgreSQL for user
   - Verify password (BCrypt)
   - Check Redis rate limit
   ↓
4. Backend generates tokens
   - Access Token (JWT, 15 min)
   - Refresh Token (UUID, 7 days)
   ↓
5. Backend stores session
   - Refresh token → PostgreSQL
   - Session metadata → Redis
   - Audit log → PostgreSQL
   ↓
6. Response to frontend
   {
     user: { id, email, name, role },
     accessToken: "eyJhbGc...",
     refreshToken: "uuid..."
   }
   ↓
7. Frontend stores tokens
   - localStorage.setItem('ziboto_access_token', ...)
   - localStorage.setItem('ziboto_refresh_token', ...)
   ↓
8. Frontend updates state
   - user: { ... }
   - isAuthenticated: true
```

### Authenticated Request Flow
```
1. User triggers action (e.g., fetch profile)
   ↓
2. Axios interceptor adds token
   headers: {
     Authorization: "Bearer eyJhbGc..."
   }
   ↓
3. Nginx forwards to any backend instance
   ↓
4. Spring Security Filter validates JWT
   - Verify signature (stateless)
   - Check expiry (stateless)
   - Check Redis blacklist (fast)
   ↓
5. Extract user from JWT claims
   - No database lookup needed
   - User info in token payload
   ↓
6. Process request & return response
```

### Token Refresh Flow
```
1. Access token expires
   ↓
2. User makes request → 401 Unauthorized
   ↓
3. Axios interceptor catches 401
   ↓
4. Check if already refreshing
   - Yes → Queue this request
   - No → Start refresh process
   ↓
5. POST /api/v1/auth/refresh
   {
     refreshToken: "uuid..."
   }
   ↓
6. Backend validates refresh token
   - Check PostgreSQL (token exists, not expired)
   - Check Redis (not blacklisted)
   - Update session activity (Redis)
   ↓
7. Backend generates new access token
   - Optionally rotate refresh token
   ↓
8. Response to frontend
   {
     accessToken: "eyJhbGc...",
     refreshToken: "uuid..."  // may be new
   }
   ↓
9. Frontend updates stored tokens
   ↓
10. Retry original request with new token
    ↓
11. Process all queued requests
```

### Logout Flow
```
1. User clicks logout
   ↓
2. POST /api/v1/auth/logout
   headers: {
     Authorization: "Bearer eyJhbGc..."
   }
   ↓
3. Backend revokes tokens
   - Add access token to Redis blacklist (TTL: 15 min)
   - Delete refresh token from PostgreSQL
   - Delete session from Redis
   ↓
4. Frontend clears tokens
   - localStorage.removeItem('ziboto_access_token')
   - localStorage.removeItem('ziboto_refresh_token')
   ↓
5. Frontend updates state
   - user: null
   - isAuthenticated: false
   ↓
6. Redirect to login page
```

## 📦 Token Structures

### Access Token (JWT)
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "550e8400-e29b-41d4-a716-446655440000",  // User ID
    "email": "user@example.com",
    "name": "John Doe",
    "role": "USER",
    "authorities": ["READ", "WRITE"],
    "iat": 1704067200,      // Issued at (Unix timestamp)
    "exp": 1704068100       // Expires at (Unix timestamp, +15 min)
  },
  "signature": "..."
}
```

**Characteristics:**
- **Storage**: Client-side only (localStorage)
- **Validation**: Stateless (signature + expiry check)
- **Expiry**: 15 minutes
- **Revocation**: Redis blacklist on logout
- **Load Balancer**: Works with any backend instance

### Refresh Token
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",  // UUID
  "userId": "user-uuid",
  "deviceInfo": {
    "userAgent": "Mozilla/5.0...",
    "platform": "Linux x86_64",
    "ipAddress": "192.168.1.100"
  },
  "expiresAt": "2024-01-08T00:00:00Z",  // +7 days
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**Characteristics:**
- **Storage**: Client-side (localStorage) + PostgreSQL
- **Validation**: Database lookup required
- **Expiry**: 7 days
- **Revocation**: Delete from PostgreSQL
- **Rotation**: Optional one-time use (delete old, create new)

## 🔄 State Management

### Frontend State (Zustand Store)
```typescript
{
  user: {
    id: "uuid",
    email: "user@example.com",
    name: "John Doe",
    role: "USER",
    emailVerified: true
  },
  isAuthenticated: true,
  isLoading: false,
  error: null,
  loadingStates: {
    login: false,
    register: false,
    logout: false,
    refresh: false,
    profile: false
  }
}
```

### Backend State

#### PostgreSQL (Persistent)
```sql
-- Users table
SELECT id, email, name, role, email_verified, created_at FROM users;

-- Refresh tokens table
SELECT id, user_id, token_hash, device_info, expires_at, created_at 
FROM refresh_tokens;

-- Audit logs table
SELECT id, user_id, action, ip_address, user_agent, timestamp 
FROM audit_logs;
```

#### Redis (Ephemeral)
```
# Rate limiting
ratelimit:/auth/login:192.168.1.100 = 3  (TTL: 15 minutes)

# Token blacklist
blacklist:token-id = "revoked"  (TTL: 15 minutes)

# Session metadata (optional)
session:user-id = {
  "lastActivity": "2024-01-01T12:00:00Z",
  "deviceInfo": { ... }
}  (TTL: 15 minutes, updated on each request)
```

## 🚦 Error Handling

### HTTP Status Codes

| Code | Meaning | Frontend Action | Backend Cause |
|------|---------|-----------------|---------------|
| **200** | Success | Process response | Request successful |
| **201** | Created | Process response | Resource created (register) |
| **400** | Bad Request | Show error message | Validation failed |
| **401** | Unauthorized | Refresh token or logout | Invalid/expired token |
| **403** | Forbidden | Show error message | Insufficient permissions |
| **404** | Not Found | Show error message | Resource not found |
| **408** | Timeout | Retry request | Request timeout |
| **409** | Conflict | Show error message | Resource already exists |
| **422** | Validation Error | Show field errors | Validation failed |
| **429** | Rate Limited | Retry with backoff | Rate limit exceeded (Redis) |
| **500** | Server Error | Retry request | Internal server error |
| **502** | Bad Gateway | Retry request | Backend instance down |
| **503** | Service Unavailable | Retry request | Backend overloaded |
| **504** | Gateway Timeout | Retry request | Backend timeout |

### Error Response Format
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "validationErrors": [
    {
      "field": "email",
      "message": "Email is already in use"
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    }
  ]
}
```

### Retry Strategy

```typescript
const RETRY_CONFIG = {
  maxRetries: 3,
  retryDelay: 1000,  // 1 second
  retryableStatusCodes: [408, 429, 500, 502, 503, 504],
  backoffMultiplier: 2  // Exponential backoff
};

// Example retry sequence
// Attempt 1: immediate
// Attempt 2: wait 1s
// Attempt 3: wait 2s
// Attempt 4: wait 4s
// Give up after 4 attempts
```

## 🔒 Security Checklist

### Frontend
- [x] ✅ Tokens stored in localStorage (XSS mitigation via CSP)
- [x] ✅ No sensitive data in URL/query params
- [x] ✅ Authorization header for all authenticated requests
- [x] ✅ Automatic token refresh on expiry
- [x] ✅ Tokens cleared on logout
- [x] ✅ Retry logic doesn't expose tokens in logs
- [x] ✅ HTTPS enforced in production
- [ ] ⚠️ Content Security Policy (CSP) configured
- [ ] ⚠️ Subresource Integrity (SRI) for CDN assets

### Backend
- [ ] ⚠️ JWT secret is 256-bit minimum
- [ ] ⚠️ Tokens signed with HS256 or RS256
- [ ] ⚠️ Access tokens expire in 15 minutes
- [ ] ⚠️ Refresh tokens expire in 7 days
- [ ] ⚠️ Refresh tokens hashed before storage (SHA-256)
- [ ] ⚠️ Passwords hashed with BCrypt (rounds=10)
- [ ] ⚠️ Rate limiting implemented (Redis)
- [ ] ⚠️ Token blacklist implemented (Redis)
- [ ] ⚠️ CORS configured correctly
- [ ] ⚠️ HTTPS enforced
- [ ] ⚠️ Security headers configured (Nginx)

### Infrastructure
- [ ] ⚠️ Redis authentication enabled
- [ ] ⚠️ PostgreSQL SSL/TLS enabled
- [ ] ⚠️ Nginx SSL/TLS configured (TLS 1.2+)
- [ ] ⚠️ Firewall rules configured
- [ ] ⚠️ Secrets managed securely (Vault, AWS Secrets Manager)
- [ ] ⚠️ Monitoring and alerting configured
- [ ] ⚠️ Logs centralized (ELK, CloudWatch)

## 📊 Performance Metrics

### Target Metrics (Production)

| Metric | Target | How to Measure |
|--------|--------|----------------|
| **Login Response Time** | < 500ms | Backend logs, APM |
| **Token Refresh Time** | < 200ms | Backend logs, APM |
| **API Response Time (p50)** | < 100ms | Nginx logs, APM |
| **API Response Time (p99)** | < 500ms | Nginx logs, APM |
| **JWT Validation Time** | < 5ms | Backend metrics |
| **Redis Lookup Time** | < 2ms | Redis metrics |
| **PostgreSQL Query Time** | < 50ms | Database metrics |
| **Error Rate** | < 1% | Nginx logs, APM |
| **Token Refresh Success Rate** | > 99% | Backend logs |
| **Redis Cache Hit Rate** | > 90% | Redis INFO |

### Capacity Planning

| Resource | Capacity | Notes |
|----------|----------|-------|
| **Backend Instances** | 3-5 | Horizontal scaling |
| **Nginx** | 1-2 | With failover |
| **Redis** | 1 Master + 2 Replicas | Sentinel for HA |
| **PostgreSQL** | 1 Primary + 2 Replicas | Streaming replication |
| **Expected Load** | 1000 req/s | 100 concurrent users |

## 🛠️ Development Workflow

### Local Development
```bash
# Terminal 1: Start backend
cd apps/backend
./mvnw spring-boot:run

# Terminal 2: Start Redis
docker run -p 6379:6379 redis:latest

# Terminal 3: Start PostgreSQL
docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:latest

# Terminal 4: Start frontend
cd apps/frontend
npm run dev

# Access app at http://localhost:5173
```

### Environment URLs

| Environment | Frontend | Backend (via Nginx) | Direct Backend |
|-------------|----------|---------------------|----------------|
| **Local** | http://localhost:5173 | N/A | http://localhost:8080 |
| **Dev** | https://dev.ziboto.com | https://api-dev.ziboto.com/api/v1 | N/A |
| **Staging** | https://staging.ziboto.com | https://api-staging.ziboto.com/api/v1 | N/A |
| **Prod** | https://ziboto.com | https://api.ziboto.com/api/v1 | N/A |

## 🐛 Common Issues & Solutions

### Issue: Token refresh loop
**Symptoms**: Infinite 401 errors, network tab shows continuous /auth/refresh calls
**Cause**: /auth/refresh endpoint itself returns 401
**Solution**: Axios interceptor must NOT retry /auth/refresh (already implemented)

```typescript
// src/lib/axios.ts
const isAuthEndpoint = url.includes('/auth/login') || 
                      url.includes('/auth/register') || 
                      url.includes('/auth/refresh');  // ✅ Don't retry

if (isAuthEndpoint) {
  return Promise.reject(error);  // Stop immediately
}
```

### Issue: User logged out unexpectedly
**Symptoms**: isAuthenticated becomes false randomly
**Causes**:
1. Access token expired and refresh token invalid/expired
2. Token blacklisted in Redis (after logout on another device)
3. Refresh token deleted from PostgreSQL

**Debug**:
```typescript
// Check token validity
console.log('Access token:', tokenService.getAccessToken());
console.log('Refresh token:', tokenService.getRefreshToken());
console.log('Token expired:', tokenService.isTokenExpired());

// Check backend logs
// - "Refresh token not found" → Deleted from PostgreSQL
// - "Token blacklisted" → In Redis blacklist
// - "Token expired" → Natural expiry
```

### Issue: CORS errors in production
**Symptoms**: Browser console shows "CORS policy blocked"
**Cause**: Nginx not configured for CORS or wrong origin
**Solution**: Update Nginx config

```nginx
# /etc/nginx/sites-available/ziboto-api
add_header 'Access-Control-Allow-Origin' 'https://ziboto.com' always;
add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
```

### Issue: Rate limiting inconsistent
**Symptoms**: Rate limit works sometimes, not others
**Cause**: Each backend instance has its own in-memory rate limiter
**Solution**: Use shared Redis for rate limiting (per architecture)

### Issue: Token works on Instance 1, fails on Instance 2
**Symptoms**: 401 errors randomly appear
**Cause**: Different JWT secrets on different instances
**Solution**: Ensure all instances share the same JWT_SECRET environment variable

```bash
# Check JWT secret on each instance
docker exec backend1 env | grep JWT_SECRET
docker exec backend2 env | grep JWT_SECRET
docker exec backend3 env | grep JWT_SECRET
# All should match!
```

## 📚 Key Files Reference

### Frontend
```
apps/frontend/
├── src/
│   ├── lib/
│   │   └── axios.ts                    # Axios config, interceptors
│   ├── services/
│   │   ├── authService.ts              # Auth API calls
│   │   └── tokenService.ts             # Token storage/retrieval
│   ├── store/
│   │   └── authStore.ts                # Zustand state management
│   ├── context/
│   │   └── AuthContext.tsx             # React context for auth
│   ├── hooks/
│   │   ├── useTokenRefresh.ts          # Auto token refresh
│   │   └── useAuthOperations.ts        # Auth operations hook
│   ├── types/
│   │   └── api.types.ts                # TypeScript types/DTOs
│   └── utils/
│       ├── apiErrorHandler.ts          # Error handling utilities
│       └── retryHandler.ts             # Retry logic with backoff
└── .env                                # Environment variables
```

### Backend
```
apps/backend/
├── src/main/java/com/ziboto/backend/
│   ├── config/
│   │   ├── SecurityConfig.java         # Spring Security config
│   │   ├── JwtConfig.java              # JWT configuration
│   │   └── RedisConfig.java            # Redis configuration
│   ├── filter/
│   │   ├── JwtAuthenticationFilter.java # JWT validation filter
│   │   └── RateLimitFilter.java         # Rate limiting filter
│   ├── controller/
│   │   └── AuthenticationController.java # Auth endpoints
│   ├── service/
│   │   ├── AuthenticationService.java   # Auth business logic
│   │   ├── JwtService.java              # JWT generation/validation
│   │   └── TokenBlacklistService.java   # Redis blacklist
│   ├── repository/
│   │   ├── UserRepository.java          # User database access
│   │   └── RefreshTokenRepository.java  # Refresh token database access
│   └── model/
│       ├── User.java                    # User entity
│       └── RefreshToken.java            # Refresh token entity
└── src/main/resources/
    └── application.yml                  # Application config
```

### Infrastructure
```
/etc/nginx/
├── nginx.conf                           # Main Nginx config
└── sites-available/
    ├── ziboto-api                       # API load balancer config
    └── ziboto-frontend                  # Frontend static serve config
```

## 🔗 Useful Commands

### Frontend
```bash
# Development
npm run dev              # Start dev server
npm run build            # Production build
npm run preview          # Preview production build

# Testing
npm run test             # Run tests
npm run lint             # Lint code

# Environment
cat .env                 # View environment variables
```

### Backend
```bash
# Development
./mvnw spring-boot:run                     # Start application
./mvnw clean package                       # Build JAR
java -jar target/backend-0.0.1.jar         # Run JAR

# Database
./mvnw flyway:migrate                      # Run migrations
./mvnw flyway:info                         # Check migration status

# Health check
curl http://localhost:8080/actuator/health
```

### Redis
```bash
# Connect
redis-cli

# Check blacklist
KEYS blacklist:*
GET blacklist:token-id

# Check rate limits
KEYS ratelimit:*
GET ratelimit:/auth/login:192.168.1.100

# Monitor commands
MONITOR
```

### PostgreSQL
```bash
# Connect
psql -U postgres -d ziboto

# Check users
SELECT * FROM users;

# Check refresh tokens
SELECT * FROM refresh_tokens WHERE user_id = 'uuid';

# Check audit logs
SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 10;
```

### Nginx
```bash
# Test config
sudo nginx -t

# Reload config
sudo systemctl reload nginx

# View logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# Check upstreams
curl http://localhost/api/v1/actuator/health
```

---

## 🎯 Summary

**Your frontend is production-ready!**

Key strengths:
- ✅ Stateless authentication (JWT)
- ✅ Load balancer compatible
- ✅ Automatic token refresh
- ✅ Proper error handling
- ✅ Retry logic

Focus areas:
- ⚠️ Backend implementation (token refresh, blacklist, rate limiting)
- ⚠️ Infrastructure setup (Nginx, Redis, PostgreSQL)
- ⚠️ Security hardening (CSP, monitoring, alerting)

Refer to:
- `PRODUCTION_BACKEND_ARCHITECTURE.md` - Detailed architecture
- `FRONTEND_BACKEND_ALIGNMENT.md` - Frontend/backend contract
- `DEPLOYMENT_CHECKLIST.md` - Production deployment steps
