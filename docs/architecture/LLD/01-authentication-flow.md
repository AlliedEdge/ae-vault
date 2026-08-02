# Low-Level Design: Authentication & Authorization Flow

## Overview
This document describes the detailed authentication and authorization mechanism for Ziboto v1 with production-ready architecture including load balancing, horizontal scaling, and distributed caching.

## Production Architecture

```
                               ┌──────────────────────┐
                               │       User           │
                               └──────────┬───────────┘
                                          │
                                          │ Login / Sign Up
                                          ▼
                          ┌────────────────────────────────┐
                          │         React Frontend         │
                          │  (Deployed on CDN/CloudFront)  │
                          └────────────────┬───────────────┘
                                          │
                                          │ HTTPS Request
                                          ▼
                ┌─────────────────────────────────────────────┐
                │  Nginx (Reverse Proxy / Load Balancer)      │
                │  ──────────────────────────────────────────  │
                │  • SSL/TLS Termination                      │
                │  • Load Balancing (Round Robin)             │
                │  • Rate Limiting (Global)                   │
                │  • Request Logging                          │
                │  • Health Checks                            │
                └────────────────┬────────────────────────────┘
                                 │
                ┌────────────────┼────────────────────────┐
                │                │                        │
                ▼                ▼                        ▼
        ┌──────────────┐   ┌──────────────┐    ┌──────────────┐
        │ Spring Boot  │   │ Spring Boot  │    │ Spring Boot  │
        │ Instance #1  │   │ Instance #2  │    │ Instance #3  │
        │──────────────│   │──────────────│    │──────────────│
        │ Port: 8081   │   │ Port: 8082   │    │ Port: 8083   │
        │ Stateless    │   │ Stateless    │    │ Stateless    │
        └──────┬───────┘   └──────┬───────┘    └──────┬───────┘
               │                  │                     │
               └──────────────┬───┴─────────────────────┘
                              │
                ┌─────────────┴───────────────┐
                │                             │
                ▼                             ▼
┌────────────────────────────────┐   ┌────────────────────────────┐
│            Redis               │   │      PostgreSQL            │
│────────────────────────────────│   │────────────────────────────│
│ • Login Rate Limiting          │   │ • users                    │
│   (IP-based: 10 req/min)       │   │   - id (UUID)              │
│                                │   │   - email                  │
│ • Signup Rate Limiting         │   │   - password_hash (BCrypt) │
│   (IP-based: 5 req/hour)       │   │   - first_name             │
│                                │   │   - last_name              │
│ • Failed Login Attempts        │   │   - is_active              │
│   (Account lock after 5)       │   │   - created_at             │
│                                │   │                            │
│ • User Session Cache           │   │ • refresh_tokens           │
│   Key: session:{userId}        │   │   - id (UUID)              │
│   TTL: 7 days                  │   │   - user_id                │
│                                │   │   - token_hash             │
│ • Blacklisted Tokens           │   │   - is_revoked             │
│   Key: blacklist:{tokenId}     │   │   - expires_at             │
│   TTL: Token expiry            │   │                            │
│                                │   │ • audit_logs               │
│ • OTP Storage (Future)         │   │   - user_id                │
│   Key: otp:{userId}            │   │   - action (LOGIN)         │
│   TTL: 5 minutes               │   │   - ip_address             │
│                                │   │   - created_at             │
│ • JWT Cache (Optional)         │   │                            │
│   Key: jwt:{userId}            │   │ • roles (Future RBAC)      │
│   TTL: 15 minutes              │   │                            │
└────────────────────────────────┘   └────────────────────────────┘
                │
                │ Generate JWT + Refresh Token
                ▼
        ┌──────────────────────────────┐
        │  JWT Token Structure         │
        │──────────────────────────────│
        │  Access Token (15 min):      │
        │  {                           │
        │    sub: userId,              │
        │    email: user@email.com,    │
        │    roles: [USER],            │
        │    type: access,             │
        │    iat: timestamp,           │
        │    exp: timestamp            │
        │  }                           │
        │                              │
        │  Refresh Token (7 days):     │
        │  {                           │
        │    sub: userId,              │
        │    type: refresh,            │
        │    iat: timestamp,           │
        │    exp: timestamp            │
        │  }                           │
        └──────────────────────────────┘
                │
                │ Return Tokens to Client
                ▼
        ┌──────────────────────────────┐
        │      React Frontend          │
        │──────────────────────────────│
        │  Store tokens in:            │
        │  • Memory (recommended)      │
        │  • localStorage (fallback)   │
        │  • httpOnly cookies (best)   │
        └──────────────────────────────┘
                │
                │ Every Future Request Uses JWT Access Token
                ▼
        ┌──────────────────────────────┐
        │  Authorization Header        │
        │  Bearer {access-token}       │
        └──────────────────────────────┘
```

## Components

### 1. Authentication Service
- **Package**: `com.ziboto.auth`
- **Key Classes**:
  - `AuthenticationController`
  - `AuthenticationService`
  - `JwtTokenProvider`
  - `CustomUserDetailsService`

### 2. Security Configuration
- **Package**: `com.ziboto.config`
- **Key Classes**:
  - `SecurityConfig`
  - `JwtAuthenticationFilter`
  - `JwtAuthenticationEntryPoint`

### 3. Load Balancing & Scaling
- **Nginx**: Round-robin load balancing
- **Multiple Spring Boot Instances**: Horizontal scaling
- **Stateless Design**: No session affinity required
- **Redis**: Distributed session/cache storage
- **PostgreSQL**: Centralized data store

## Detailed Authentication Flow Diagram

```
┌─────────┐                 ┌──────────────┐                ┌─────────────┐
│         │   POST /login   │              │   Validate     │             │
│ Client  │────────────────>│ Auth         │───────────────>│ User        │
│         │   credentials   │ Controller   │   credentials  │ Service     │
└─────────┘                 └──────────────┘                └─────────────┘
     │                              │                               │
     │                              │<──────────────────────────────┘
     │                              │        User Entity
     │                              │
     │                              v
     │                       ┌──────────────┐
     │                       │ JWT Token    │
     │                       │ Provider     │
     │                       └──────────────┘
     │                              │
     │                              │ Generate JWT
     │                              │ - Access Token (15 min)
     │                              │ - Refresh Token (7 days)
     │                              v
     │                       ┌──────────────┐
     │                       │ Redis Cache  │
     │                       │ Store Session│
     │                       └──────────────┘
     │                              │
     │<─────────────────────────────┘
     │        Return JWT Tokens
     v
## Detailed Authentication Flow Diagram

### Step-by-Step Login Flow

```
STEP 1: User Login Request
──────────────────────────────────────────────────────────────
┌──────────┐
│  Client  │  POST /api/v1/auth/login
│          │  {
└────┬─────┘    "email": "user@example.com",
     │          "password": "SecurePass123!"
     │        }
     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Nginx Load Balancer                      │
│                                                             │
│  1. SSL/TLS Termination                                     │
│  2. Check global rate limit (1000 req/sec)                 │
│  3. Log request                                             │
│  4. Route to available backend (Round Robin)               │
└─────────────────────────────────────────────────────────────┘
     │
     │ Forward to Backend Instance #2 (least loaded)
     ▼
┌─────────────────────────────────────────────────────────────┐
│            Spring Boot Instance #2 (Port 8082)              │
│─────────────────────────────────────────────────────────────│
│  AuthenticationController.login()                           │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 2: Rate Limiting Check
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cache Check                        │
│                                                             │
│  Key: rate_limit:login:{ip_address}                        │
│  Current: 5 attempts                                        │
│  Limit: 10 per minute                                       │
│  ✓ Check passed                                            │
│                                                             │
│  Increment counter: 5 → 6                                  │
│  TTL: 60 seconds                                           │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 3: Failed Login Attempts Check
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cache Check                        │
│                                                             │
│  Key: failed_attempts:{email}                              │
│  Current: 2 attempts                                        │
│  Lock Threshold: 5 attempts                                 │
│  ✓ Account not locked                                      │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 4: Retrieve User from Database
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL Query                         │
│                                                             │
│  SELECT * FROM users WHERE email = 'user@example.com'      │
│                                                             │
│  Result:                                                    │
│    id: 550e8400-e29b-41d4-a716-446655440000               │
│    email: user@example.com                                 │
│    password_hash: $2a$12$...                               │
│    is_active: true                                         │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 5: Validate Password
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│              BCrypt Password Verification                   │
│                                                             │
│  Provided: "SecurePass123!"                                │
│  Stored Hash: $2a$12$...                                   │
│                                                             │
│  BCrypt.checkpw(provided, stored)                          │
│  ✓ Password matches                                        │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 6: Clear Failed Attempts (on success)
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cache Update                       │
│                                                             │
│  DEL failed_attempts:user@example.com                      │
│  ✓ Counter reset                                           │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 7: Generate JWT Tokens
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│               JwtTokenProvider.generateTokens()             │
│                                                             │
│  Access Token:                                              │
│    Algorithm: HS256                                         │
│    Secret: ${JWT_SECRET}                                    │
│    Claims: {                                                │
│      sub: "550e8400-e29b-41d4-a716-446655440000",         │
│      email: "user@example.com",                            │
│      roles: ["USER"],                                       │
│      type: "access",                                        │
│      iat: 1722594600,                                       │
│      exp: 1722595500  // 15 min                            │
│    }                                                        │
│    Token: eyJhbGciOiJIUzI1NiIs...                          │
│                                                             │
│  Refresh Token:                                             │
│    Claims: {                                                │
│      sub: "550e8400-e29b-41d4-a716-446655440000",         │
│      type: "refresh",                                       │
│      iat: 1722594600,                                       │
│      exp: 1723199400  // 7 days                            │
│    }                                                        │
│    Token: eyJhbGciOiJIUzI1NiIs...                          │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 8: Store Session in Redis
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cache Store                        │
│                                                             │
│  Key: session:550e8400-e29b-41d4-a716-446655440000        │
│  TTL: 7 days (604800 seconds)                              │
│  Value: {                                                   │
│    "userId": "550e8400-e29b-41d4-a716-446655440000",      │
│    "email": "user@example.com",                            │
│    "roles": ["USER"],                                       │
│    "lastAccess": "2026-08-02T10:30:00Z",                   │
│    "ipAddress": "192.168.1.100",                           │
│    "userAgent": "Mozilla/5.0..."                           │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 9: Store Refresh Token in Database
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│                  PostgreSQL Insert                          │
│                                                             │
│  INSERT INTO refresh_tokens (                               │
│    id, user_id, token_hash, expires_at, device_type        │
│  ) VALUES (                                                 │
│    'token-uuid',                                            │
│    '550e8400-e29b-41d4-a716-446655440000',                │
│    '$2a$12$...' // Hashed refresh token                    │
│    '2026-08-09T10:30:00Z',                                 │
│    'WEB'                                                    │
│  )                                                          │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 10: Update Last Login Time
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│                  PostgreSQL Update                          │
│                                                             │
│  UPDATE users                                               │
│  SET last_login_at = '2026-08-02T10:30:00Z'               │
│  WHERE id = '550e8400-e29b-41d4-a716-446655440000'        │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 11: Create Audit Log
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│                  PostgreSQL Insert                          │
│                                                             │
│  INSERT INTO audit_logs (                                   │
│    user_id, action, ip_address, user_agent, status         │
│  ) VALUES (                                                 │
│    '550e8400-e29b-41d4-a716-446655440000',                │
│    'LOGIN',                                                 │
│    '192.168.1.100',                                        │
│    'Mozilla/5.0...',                                        │
│    'SUCCESS'                                                │
│  )                                                          │
└─────────────────────────────────────────────────────────────┘
     │
     ▼

STEP 12: Return Response to Client
──────────────────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────┐
│              HTTP 200 OK Response                           │
│                                                             │
│  {                                                          │
│    "success": true,                                         │
│    "data": {                                                │
│      "accessToken": "eyJhbGciOiJIUzI1NiIs...",            │
│      "refreshToken": "eyJhbGciOiJIUzI1NiIs...",           │
│      "tokenType": "Bearer",                                 │
│      "expiresIn": 900,  // 15 minutes                      │
│      "user": {                                              │
│        "userId": "550e8400-e29b-41d4-a716-446655440000",  │
│        "email": "user@example.com",                        │
│        "firstName": "John",                                 │
│        "lastName": "Doe",                                   │
│        "storageQuota": 5368709120,                         │
│        "storageUsed": 1234567890                           │
│      }                                                      │
│    }                                                        │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
     │
     │ Response flows back through Nginx
     ▼
┌──────────┐
│  Client  │  ✓ Tokens received
│          │  ✓ Store in memory/localStorage
└──────────┘  ✓ Ready for authenticated requests
```

## Request/Response Flow

### 1. User Registration

**Endpoint**: `POST /api/v1/auth/register`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response**:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": "uuid-string",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "createdAt": "2026-08-02T10:30:00Z"
  }
}
```

**Processing Steps**:
1. Validate input data (email format, password strength)
2. Check if email already exists
3. Hash password using BCrypt (cost factor: 12)
4. Create User entity
5. Save to PostgreSQL
6. Return success response

### 2. User Login

**Endpoint**: `POST /api/v1/auth/login`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "userId": "uuid-string",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe"
    }
  }
}
```

**Processing Steps**:
1. Retrieve user by email from PostgreSQL
2. Verify password using BCrypt
3. Generate JWT access token (15 min expiry)
4. Generate JWT refresh token (7 days expiry)
5. Store session in Redis with user metadata
6. Return tokens and user info

### 3. Token Refresh

**Endpoint**: `POST /api/v1/auth/refresh`

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response**: Same as login response with new tokens

## Class Diagram

```
┌──────────────────────────┐
│  AuthenticationController│
├──────────────────────────┤
│ - authService            │
├──────────────────────────┤
│ + register()             │
│ + login()                │
│ + refresh()              │
│ + logout()               │
└──────────────────────────┘
            │
            │ uses
            v
┌──────────────────────────┐
│  AuthenticationService   │
├──────────────────────────┤
│ - userRepository         │
│ - jwtTokenProvider       │
│ - passwordEncoder        │
│ - redisTemplate          │
├──────────────────────────┤
│ + registerUser()         │
│ + authenticateUser()     │
│ + refreshToken()         │
│ + logout()               │
└──────────────────────────┘
            │
            │ uses
            v
┌──────────────────────────┐
│    JwtTokenProvider      │
├──────────────────────────┤
│ - SECRET_KEY             │
│ - ACCESS_TOKEN_VALIDITY  │
│ - REFRESH_TOKEN_VALIDITY │
├──────────────────────────┤
│ + generateAccessToken()  │
│ + generateRefreshToken() │
│ + validateToken()        │
│ + getUserIdFromToken()   │
│ + getExpirationDate()    │
└──────────────────────────┘
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    is_email_verified BOOLEAN DEFAULT false,
    storage_quota_bytes BIGINT DEFAULT 5368709120, -- 5GB
    storage_used_bytes BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_active (is_active)
);
```

## Redis Cache Structure

### Session Data
```
Key: session:{userId}
TTL: 7 days
Value: {
  "userId": "uuid",
  "email": "user@example.com",
  "roles": ["USER"],
  "lastAccess": "2026-08-02T10:30:00Z"
}
```

### Refresh Token
```
Key: refresh_token:{tokenId}
TTL: 7 days
Value: {
  "userId": "uuid",
  "issuedAt": "2026-08-02T10:30:00Z"
}
```

## Security Filters

### JWT Authentication Filter
```
Request → JwtAuthenticationFilter → SecurityContext → Controller
```

**Filter Logic**:
1. Extract JWT from Authorization header
2. Validate token signature and expiration
3. Extract user ID from token
4. Load user details from cache/database
5. Set SecurityContext with Authentication
6. Continue filter chain

## JWT Token Structure

### Access Token Claims
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "roles": ["USER"],
  "type": "access",
  "iat": 1722594600,
  "exp": 1722595500
}
```

### Refresh Token Claims
```json
{
  "sub": "user-uuid",
  "type": "refresh",
  "iat": 1722594600,
  "exp": 1723199400
}
```

## Error Handling

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| AUTH_001 | 400 | Invalid credentials |
| AUTH_002 | 401 | Token expired |
| AUTH_003 | 401 | Invalid token |
| AUTH_004 | 409 | Email already exists |
| AUTH_005 | 403 | Account not verified |
| AUTH_006 | 403 | Account disabled |

## Configuration Properties

```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-validity: 900000  # 15 minutes in ms
  refresh-token-validity: 604800000  # 7 days in ms

spring:
  security:
    password:
      bcrypt-strength: 12
```

## Performance Considerations

### Load Balancing Strategy

**Nginx Configuration**:
```nginx
upstream spring_boot_backend {
    least_conn;  # Route to instance with fewest connections
    
    server spring-boot-1:8081 max_fails=3 fail_timeout=30s;
    server spring-boot-2:8082 max_fails=3 fail_timeout=30s;
    server spring-boot-3:8083 max_fails=3 fail_timeout=30s;
    
    keepalive 32;  # Keep connections alive
}

server {
    listen 443 ssl http2;
    server_name api.ziboto.com;
    
    # SSL Configuration
    ssl_certificate /etc/ssl/certs/ziboto.crt;
    ssl_certificate_key /etc/ssl/private/ziboto.key;
    
    # Rate Limiting
    limit_req_zone $binary_remote_addr zone=login:10m rate=10r/m;
    limit_req zone=login burst=5 nodelay;
    
    location /api/v1/auth {
        proxy_pass http://spring_boot_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 5s;
        proxy_send_timeout 10s;
        proxy_read_timeout 10s;
    }
}
```

### Horizontal Scaling

**Spring Boot Configuration** (`application.yml`):
```yaml
server:
  port: ${SERVER_PORT:8080}  # Different per instance

spring:
  session:
    store-type: redis  # Centralized session store
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
  
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10  # Per instance
      minimum-idle: 5
      connection-timeout: 30000

jwt:
  secret: ${JWT_SECRET}  # Same across all instances
  access-token-validity: 900000  # 15 minutes
  refresh-token-validity: 604800000  # 7 days
```

### Stateless Design Benefits

1. **No Session Affinity Required**: Any instance can handle any request
2. **Easy Scaling**: Add/remove instances without data migration
3. **Fault Tolerance**: Instance failure doesn't lose sessions
4. **Load Distribution**: Nginx can balance freely
5. **Zero Downtime Deployments**: Rolling updates possible

### Subsequent Authenticated Request Flow

```
AUTHENTICATED REQUEST (File Upload)
──────────────────────────────────────────────────────────────
┌──────────┐
│  Client  │  POST /api/v1/files/upload
│          │  Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
└────┬─────┘
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Nginx Load Balancer                      │
│  Routes to: Spring Boot Instance #1                        │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│            Spring Boot Instance #1 (Port 8081)              │
│  JwtAuthenticationFilter intercepts request                 │
└─────────────────────────────────────────────────────────────┘
     │
     │ 1. Extract JWT from Authorization header
     ▼
┌─────────────────────────────────────────────────────────────┐
│               JwtTokenProvider.validateToken()              │
│                                                             │
│  1. Parse JWT                                               │
│  2. Verify signature with ${JWT_SECRET}                     │
│  3. Check expiration                                        │
│  4. Extract claims (userId, email, roles)                   │
│  ✓ Token valid                                             │
└─────────────────────────────────────────────────────────────┘
     │
     │ 2. Check if token is blacklisted (logout)
     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cache Check                        │
│                                                             │
│  Key: blacklist:{tokenId}                                   │
│  Result: NOT FOUND (token not blacklisted)                  │
│  ✓ Token active                                            │
└─────────────────────────────────────────────────────────────┘
     │
     │ 3. Load user session (optional, for user context)
     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cache Get                          │
│                                                             │
│  Key: session:{userId}                                      │
│  Returns: User session data                                 │
│  Update lastAccess timestamp                                │
└─────────────────────────────────────────────────────────────┘
     │
     │ 4. Set SecurityContext with Authentication
     ▼
┌─────────────────────────────────────────────────────────────┐
│         SecurityContextHolder.setAuthentication()           │
│                                                             │
│  UserPrincipal: {                                           │
│    userId: 550e8400-e29b-41d4-a716-446655440000,          │
│    email: user@example.com,                                │
│    authorities: [ROLE_USER]                                 │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
     │
     │ 5. Continue filter chain → Controller
     ▼
┌─────────────────────────────────────────────────────────────┐
│              FileUploadController.uploadFile()              │
│  @AuthenticationPrincipal UserPrincipal user                │
│  ✓ User authenticated                                      │
│  ✓ Process file upload                                     │
└─────────────────────────────────────────────────────────────┘

Total Time: ~50ms (mostly JWT validation, no DB queries needed!)
```

## Performance Considerations

1. **Redis Caching**: User sessions cached for fast validation
2. **Password Hashing**: BCrypt with cost factor 12 (balance security/performance)
3. **Token Validation**: Stateless JWT validation (no DB lookup per request)
4. **Connection Pooling**: PostgreSQL connection pool (min: 5, max: 20)

## Testing Strategy

### High Availability & Disaster Recovery

#### Component Redundancy
```
┌─────────────────────────────────────────────────────────────┐
│              HIGH AVAILABILITY SETUP                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Nginx (Load Balancer):                                     │
│    • Primary: nginx-1 (Active)                             │
│    • Secondary: nginx-2 (Standby, keepalived)              │
│    • Health check every 5s                                  │
│    • Automatic failover < 10s                              │
│                                                             │
│  Spring Boot Instances:                                     │
│    • Minimum 3 instances (N+1 redundancy)                  │
│    • Can lose 1 instance without impact                    │
│    • Auto-scaling based on CPU/Memory                      │
│    • Rolling deployments (zero downtime)                    │
│                                                             │
│  Redis:                                                     │
│    • Redis Sentinel (3 nodes)                              │
│      - Master: redis-1 (read/write)                        │
│      - Replica: redis-2 (read-only)                        │
│      - Replica: redis-3 (read-only)                        │
│    • Automatic master failover                             │
│    • Data persistence: RDB + AOF                           │
│                                                             │
│  PostgreSQL:                                                │
│    • Primary-Standby replication                           │
│      - Primary: postgres-1 (read/write)                    │
│      - Standby: postgres-2 (read-only, sync replication)   │
│    • Automatic failover with Patroni/Repmgr               │
│    • Point-in-time recovery (PITR)                         │
│    • Daily backups to S3                                    │
└─────────────────────────────────────────────────────────────┘
```

#### Disaster Recovery Strategy

**Recovery Time Objective (RTO)**: 15 minutes  
**Recovery Point Objective (RPO)**: 5 minutes  

**Backup Schedule**:
```
PostgreSQL:
  • Full backup: Daily at 2 AM UTC
  • Incremental: Every 6 hours
  • WAL archiving: Continuous
  • Retention: 30 days
  • Storage: AWS S3 (cross-region)

Redis:
  • RDB snapshots: Every hour
  • AOF: Every second
  • Retention: 7 days
  • Storage: EBS volumes (replicated)

Application Logs:
  • Streaming to CloudWatch Logs
  • Archived to S3 after 30 days
  • Retention: 1 year
```

#### Health Checks

**Nginx Health Check**:
```nginx
location /health {
    access_log off;
    return 200 "healthy\n";
    add_header Content-Type text/plain;
}
```

**Spring Boot Actuator**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

**Health Check Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "SELECT 1"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 107374182400,
        "free": 53687091200
      }
    }
  }
}
```

### Monitoring & Alerting

#### Metrics to Track

**Authentication Metrics**:
```
• Login success rate (target: >99%)
• Login response time (target: <500ms p95)
• Failed login attempts (alert: >100/min)
• JWT validation time (target: <10ms)
• Redis hit rate (target: >95%)
• Account lockouts (alert: >10/min)
• Concurrent sessions per user
• Token refresh rate
```

**Infrastructure Metrics**:
```
• CPU usage per instance (alert: >80%)
• Memory usage (alert: >85%)
• Connection pool usage (alert: >90%)
• Request queue depth (alert: >100)
• Response time per endpoint
• Error rate (alert: >1%)
```

#### Alert Rules

**Critical Alerts** (PagerDuty):
```
• All backend instances down
• Database connection failed
• Redis connection failed
• Error rate > 5%
• Response time > 2s (p95)
```

**Warning Alerts** (Slack):
```
• Single backend instance down
• Redis memory > 80%
• Database connections > 90%
• Disk space < 20%
• Failed logins > 50/min
```

### Security Monitoring

```
┌─────────────────────────────────────────────────────────────┐
│              SECURITY MONITORING & ALERTS                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Suspicious Activity Detection:                             │
│    • Multiple failed logins (>5 in 5 min)                  │
│    • Login from new location/device                        │
│    • Concurrent logins from different IPs                  │
│    • Token reuse after logout                              │
│    • Expired token usage attempts                          │
│    • Abnormal API usage patterns                           │
│                                                             │
│  Automated Responses:                                       │
│    • Account temporary lock (30 min)                       │
│    • CAPTCHA challenge                                      │
│    • Email notification to user                            │
│    • Admin dashboard alert                                  │
│    • IP address blocking (repeat offenders)                │
│                                                             │
│  Audit Logging:                                             │
│    • All authentication events                             │
│    • Token generation/refresh/revocation                    │
│    • Account lock/unlock                                    │
│    • Password changes                                       │
│    • Retention: 1 year (compliance)                        │
└─────────────────────────────────────────────────────────────┘
```

## Testing Strategy

- **Unit Tests**: Mock dependencies, test each method
- **Integration Tests**: Test with real Redis and PostgreSQL (Testcontainers)
- **Security Tests**: Test token manipulation, expired tokens, invalid credentials
- **Load Tests**: Simulate concurrent login requests

---

**Version**: 2.0 - Production Ready  
**Last Updated**: 2026-08-02  
**Author**: Ziboto Team  
**Status**: Production-Grade Architecture with HA/DR

**Related Documents**:
- [Production Upload Flow](07-production-upload-flow.md)
- [Caching Strategy](03-caching-strategy.md)
- [Database Schema](04-database-schema.md)
- [API Specifications](05-api-specifications.md)
