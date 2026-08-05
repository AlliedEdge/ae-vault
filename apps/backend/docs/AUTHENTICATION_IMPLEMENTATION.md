# Authentication & Authorization Implementation Guide

## Overview

This document describes the complete production-ready authentication and authorization implementation for Ziboto v1. The system is designed for horizontal scaling with load balancing, distributed caching, and comprehensive security features.

**Architecture Highlights:**
- **Stateless JWT Authentication** - No server-side sessions, fully scalable
- **Distributed Redis Cache** - Session data, rate limiting, token blacklisting
- **Horizontal Scaling** - Support for multiple backend instances
- **Load Balancing** - Nginx with least-connections strategy
- **Rate Limiting** - Endpoint-specific limits to prevent abuse
- **Security Features** - BCrypt hashing, token rotation, account lockout
- **Health Monitoring** - Custom health indicators for all components
- **Audit Logging** - Complete security event tracking

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Authentication Flow](#authentication-flow)
3. [Component Details](#component-details)
4. [API Endpoints](#api-endpoints)
5. [Configuration](#configuration)
6. [Security Features](#security-features)
7. [Deployment Guide](#deployment-guide)
8. [Monitoring & Health Checks](#monitoring--health-checks)
9. [Testing](#testing)
10. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### System Architecture

```
┌──────────────┐
│    Client    │ (React, Mobile, etc.)
└──────┬───────┘
       │ HTTPS
       ▼
┌──────────────────────────────────────┐
│       Nginx Load Balancer            │
│  • SSL/TLS Termination               │
│  • Rate Limiting (Global)            │
│  • Load Balancing (least_conn)       │
│  • Health Checks                     │
└──────────────┬───────────────────────┘
               │
       ┌───────┼───────┐
       ▼       ▼       ▼
   ┌──────────┐  ┌──────────┐  ┌──────────┐
   │ Spring   │  │ Spring   │  │ Spring   │
   │ Boot #1  │  │ Boot #2  │  │ Boot #3  │
   │ :8081    │  │ :8082    │  │ :8083    │
   └────┬─────┘  └────┬─────┘  └────┬─────┘
        │             │             │
        └─────────────┼─────────────┘
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
   ┌─────────────┐         ┌──────────────┐
   │   Redis     │         │  PostgreSQL  │
   │  (Cache)    │         │  (Database)  │
   └─────────────┘         └──────────────┘
```

### Key Components

| Component | Purpose | Technology |
|-----------|---------|------------|
| **Nginx** | Load balancer, SSL termination, rate limiting | Nginx Alpine |
| **Spring Boot** | Application servers (3+ instances) | Java 17, Spring Boot 3.x |
| **Redis** | Session cache, rate limits, token blacklist | Redis 7 |
| **PostgreSQL** | User data, refresh tokens, audit logs | PostgreSQL 15 |
| **JWT** | Stateless authentication tokens | JJWT (io.jsonwebtoken) |

---

## Authentication Flow

### 1. User Registration Flow


**Endpoint:** `POST /api/v1/auth/register`

**Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "username": "johndoe",
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe"
    }
  }
}
```

**Process Steps:**
1. Validate request data (email format, password strength)
2. Check if username/email already exists
3. Hash password with BCrypt (strength: 10)
4. Create user entity and save to PostgreSQL
5. Generate access token (15 min) and refresh token (7 days)
6. Store hashed refresh token in PostgreSQL
7. Cache user session in Redis (TTL: 1 hour)
8. Return tokens and user information


### 2. User Login Flow (Detailed)

**Endpoint:** `POST /api/v1/auth/login`

**Request:**
```json
{
  "usernameOrEmail": "john@example.com",
  "password": "SecurePass123!"
}
```

**Complete Flow (13 Steps):**

```
STEP 1: Client Request → Nginx Load Balancer
  ↓ Route to available backend (least connections)

STEP 2: Rate Limit Check (Redis)
  ↓ Key: rate_limit:login:{IP}
  ↓ Limit: 10 requests/minute
  ✓ Allow if within limit

STEP 3: Failed Login Check (Redis)
  ↓ Key: failed_login:{email}
  ↓ Lock if ≥5 failed attempts
  ✓ Proceed if not locked

STEP 4: Retrieve User (PostgreSQL)
  ↓ Query: SELECT * FROM users WHERE email = ?
  ✓ User found

STEP 5: Verify Password (BCrypt)
  ↓ BCrypt.checkpw(provided, stored_hash)
  ✓ Password matches

STEP 6: Reset Failed Attempts (Redis)
  ↓ DELETE failed_login:{email}
  ✓ Counter cleared

STEP 7: Generate Access Token (JWT)
  ↓ Claims: {sub, email, roles, type: "access"}
  ↓ Expiry: 15 minutes
  ✓ Token generated

STEP 8: Generate Refresh Token (JWT)
  ↓ Claims: {sub, type: "refresh"}
  ↓ Expiry: 7 days
  ✓ Token generated

STEP 9: Store Session (Redis)
  ↓ Key: session:{userId}
  ↓ TTL: 7 days
  ✓ Session cached

STEP 10: Store Refresh Token (PostgreSQL)
  ↓ INSERT INTO refresh_tokens (user_id, token_hash, ...)
  ↓ Hash token with BCrypt before storage
  ✓ Token stored

STEP 11: Update Last Login (PostgreSQL)
  ↓ UPDATE users SET last_login_at = NOW()
  ✓ Login time updated

STEP 12: Create Audit Log (PostgreSQL)
  ↓ INSERT INTO audit_logs (action: LOGIN, ...)
  ✓ Event logged

STEP 13: Return Response → Client
  ✓ Tokens + User Info
```

**Total Time:** ~100-200ms (depending on load)


### 3. Token Refresh Flow

**Endpoint:** `POST /api/v1/auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response:** Same as login response with new tokens

**Process Steps:**
1. Validate refresh token JWT format
2. Check if token is blacklisted (Redis)
3. Extract username from token
4. Check refresh rate limit (10 attempts/hour)
5. Find matching hashed token in PostgreSQL
6. Verify token is not revoked or expired
7. Generate new access token (15 min)
8. Generate new refresh token (7 days) - **Token Rotation**
9. Revoke old refresh token in PostgreSQL
10. Update session in Redis
11. Create audit log (TOKEN_REFRESH)
12. Return new tokens

**Security: Token Rotation** - Old refresh token is invalidated when new one is issued.

### 4. Logout Flow

**Endpoint:** `POST /api/v1/auth/logout`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Process Steps:**
1. Extract access token from Authorization header
2. Blacklist access token in Redis (TTL: remaining token life)
3. Find and revoke all user's refresh tokens in PostgreSQL
4. Clear session cache in Redis
5. Remove active session tracking
6. Create audit log (LOGOUT)

**Note:** Logout always succeeds even if errors occur (fail-safe)


### 5. Authenticated Request Flow

**Example:** `GET /api/v1/files`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Process (JwtAuthenticationFilter):**
```
1. Extract JWT from Authorization header
   ↓
2. Validate token signature and expiration
   ↓ JwtTokenProvider.validateAccessToken()
   ✓ Token valid

3. Check if token is blacklisted (Redis)
   ↓ TokenBlacklistService.isTokenBlacklisted()
   ✓ Not blacklisted

4. Extract username from token claims
   ↓ JwtTokenProvider.getUsernameFromToken()
   ✓ Username: "johndoe"

5. Load user details from database
   ↓ UserDetailsService.loadUserByUsername()
   ✓ User loaded with authorities

6. Create Authentication object
   ↓ UsernamePasswordAuthenticationToken
   ✓ Authorities: [ROLE_USER]

7. Set SecurityContext
   ↓ SecurityContextHolder.setAuthentication()
   ✓ User authenticated for request

8. Continue to controller
   ↓ Request processed
   ✓ Response returned
```

**Performance:** ~10-50ms (mostly JWT validation, no DB query needed!)

---

## Component Details

### JWT Token Structure

**Access Token (15 minutes):**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john@example.com",
  "roles": ["USER"],
  "type": "access",
  "iat": 1722594600,
  "exp": 1722595500,
  "iss": "ziboto",
  "aud": "ziboto-api"
}
```

**Refresh Token (7 days):**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "type": "refresh",
  "iat": 1722594600,
  "exp": 1723199400,
  "iss": "ziboto",
  "aud": "ziboto-api"
}
```


### Redis Cache Structure

#### Session Cache
```
Key Pattern: session:{userId}
TTL: 7 days (604800 seconds)
Value: {
  "userId": "550e8400-...",
  "email": "john@example.com",
  "roles": ["USER"],
  "lastAccess": "2026-08-05T10:30:00Z",
  "ipAddress": "192.168.1.100"
}
```

#### Rate Limiting
```
Key Pattern: rate_limit:login:{ipAddress}
TTL: 15 minutes (900 seconds)
Value: Request count (incremented on each attempt)
Limit: 10 requests per 15 minutes
```

#### Failed Login Attempts
```
Key Pattern: failed_login:{email}
TTL: 1 hour (3600 seconds)
Value: Failed attempt count
Lockout: 5 attempts = 30 minute lock
```

#### Token Blacklist
```
Key Pattern: token:blacklist:{tokenId}
TTL: Token expiry time (15 minutes for access token)
Value: Reason for blacklist (e.g., "LOGOUT")
```

### PostgreSQL Schema

#### users table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(60) NOT NULL,  -- BCrypt hash
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    storage_quota_bytes BIGINT DEFAULT 5368709120,
    storage_used_bytes BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_login_at TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
);
```

#### refresh_tokens table
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(60) UNIQUE NOT NULL,  -- BCrypt hash
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    last_used_at TIMESTAMP,
    
    INDEX idx_token_hash (token_hash),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);
```


---

## API Endpoints

### Authentication Endpoints

| Endpoint | Method | Auth | Rate Limit | Description |
|----------|--------|------|------------|-------------|
| `/api/v1/auth/register` | POST | ❌ | 5/hour | Register new user |
| `/api/v1/auth/login` | POST | ❌ | 10/min | Login with credentials |
| `/api/v1/auth/refresh` | POST | ❌ | 20/min | Refresh access token |
| `/api/v1/auth/logout` | POST | ✅ | 100/min | Logout and revoke tokens |
| `/api/v1/auth/verify` | GET | ✅ | 100/min | Verify token validity |

### Health Check Endpoints

| Endpoint | Auth | Description |
|----------|------|-------------|
| `/api/v1/health` | ❌ | Simple health check |
| `/api/v1/health/detailed` | ❌ | Detailed health with metrics |
| `/api/v1/health/ready` | ❌ | Kubernetes readiness probe |
| `/api/v1/health/live` | ❌ | Kubernetes liveness probe |
| `/actuator/health` | ❌ | Spring Boot Actuator health |

### Example Requests

#### Register
```bash
curl -X POST http://localhost/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

#### Login
```bash
curl -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john@example.com",
    "password": "SecurePass123!"
  }'
```

#### Refresh Token
```bash
curl -X POST http://localhost/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

#### Logout
```bash
curl -X POST http://localhost/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

#### Authenticated Request
```bash
curl -X GET http://localhost/api/v1/files \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```


---

## Configuration

### Environment Variables

**Required:**
```bash
# JWT Configuration
JWT_SECRET=your_base64_encoded_secret_key_min_256_bits

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/ziboto
DATABASE_USERNAME=ziboto
DATABASE_PASSWORD=strong_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# Server
SERVER_PORT=8080
```

**Optional (with defaults):**
```bash
# JWT Expiry
JWT_EXPIRATION=900000                    # 15 minutes
JWT_REFRESH_EXPIRATION=604800000         # 7 days

# Rate Limiting
REDIS_RATE_LIMIT_LOGIN_MAX=10            # 10 attempts
REDIS_RATE_LIMIT_LOGIN_WINDOW=15         # per 15 minutes
REDIS_RATE_LIMIT_SIGNUP_MAX=5            # 5 attempts
REDIS_RATE_LIMIT_SIGNUP_WINDOW=60        # per hour

# Failed Login
REDIS_FAILED_LOGIN_MAX=5                 # 5 failed attempts
REDIS_FAILED_LOGIN_LOCKOUT=30            # 30 minute lockout

# Database Pool
DATABASE_POOL_SIZE=10                    # per instance
```

### application.yml Configuration

**Key Settings:**
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 20    # Connection pool
          max-idle: 10
          min-idle: 5

  datasource:
    hikari:
      maximum-pool-size: 10    # Per instance
      minimum-idle: 5

app:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 900000       # 15 min
      refresh-expiration: 604800000  # 7 days
```


---

## Security Features

### 1. Password Security
- **BCrypt Hashing**: Strength 10 (1024 rounds)
- **Salt**: Automatically generated per password
- **No Plain Text**: Passwords never stored in plain text
- **Validation**: Minimum requirements enforced

### 2. Token Security
- **JWT Signature**: HS512 algorithm with secret key
- **Token Types**: Separate access and refresh tokens
- **Token Rotation**: Refresh tokens invalidated on use
- **Blacklisting**: Revoked tokens stored in Redis
- **Short Expiry**: Access tokens expire in 15 minutes

### 3. Rate Limiting

| Endpoint | Limit | Window | Burst |
|----------|-------|--------|-------|
| Login | 10 requests | 1 minute | 5 |
| Registration | 5 requests | 1 hour | 2 |
| Token Refresh | 20 requests | 1 minute | 10 |
| General API | 100 requests | 1 minute | 30 |

### 4. Account Protection
- **Failed Login Tracking**: Per email address
- **Account Lockout**: 5 failed attempts = 30 min lock
- **IP Tracking**: Monitor suspicious activity
- **Audit Logging**: All security events logged

### 5. Session Management
- **Stateless**: No server-side sessions
- **Distributed Cache**: Redis for session data
- **TTL Management**: Automatic session expiry
- **Concurrent Sessions**: Configurable limits

### 6. HTTPS & Headers
- **TLS 1.2/1.3**: Modern encryption only
- **HSTS**: HTTP Strict Transport Security
- **CSP**: Content Security Policy
- **X-Frame-Options**: Clickjacking protection
- **X-Content-Type-Options**: MIME sniffing protection

---

## Deployment Guide

### Docker Compose Deployment

**1. Clone and setup:**
```bash
cd /home/rayan/Projects/ziboto/apps/backend
```

**2. Create `.env` file:**
```bash
cat > .env << EOF
DB_PASSWORD=your_secure_db_password
REDIS_PASSWORD=your_secure_redis_password
JWT_SECRET=$(openssl rand -base64 64)
EOF
```

**3. Generate SSL certificates:**
```bash
# Development (self-signed)
mkdir -p nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/ziboto.key \
  -out nginx/ssl/ziboto.crt \
  -subj "/CN=localhost"

# Production (Let's Encrypt)
# Configure in docker-compose and use certbot
```


**4. Build application:**
```bash
./mvnw clean package -DskipTests
docker build -t ziboto-backend:latest .
```

**5. Start services:**
```bash
cd nginx
docker-compose -f docker-compose-nginx.yml up -d
```

**6. Verify deployment:**
```bash
# Check service status
docker-compose ps

# Check health
curl http://localhost/api/v1/health

# Check logs
docker logs -f ziboto-nginx
docker logs -f spring-boot-1
```

### Kubernetes Deployment

**Deployment manifest:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ziboto-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ziboto-backend
  template:
    metadata:
      labels:
        app: ziboto-backend
    spec:
      containers:
      - name: backend
        image: ziboto-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: ziboto-secrets
              key: jwt-secret
        - name: DATABASE_URL
          value: jdbc:postgresql://postgres:5432/ziboto
        - name: REDIS_HOST
          value: redis
        readinessProbe:
          httpGet:
            path: /api/v1/health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /api/v1/health/live
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
```

### Scaling Horizontally

**Add backend instances:**
```bash
# Docker Compose
docker-compose -f docker-compose-nginx.yml up -d --scale spring-boot=5

# Kubernetes
kubectl scale deployment ziboto-backend --replicas=5
```

**Update Nginx upstream** (for Docker):
```nginx
upstream spring_boot_backend {
    least_conn;
    server spring-boot-1:8081;
    server spring-boot-2:8082;
    server spring-boot-3:8083;
    server spring-boot-4:8084;  # New
    server spring-boot-5:8085;  # New
}
```


---

## Monitoring & Health Checks

### Health Indicators

**Custom Indicators:**
1. **RedisHealthIndicator** - Redis connectivity, memory, version
2. **DatabaseHealthIndicator** - PostgreSQL connectivity, size, connections
3. **AuthenticationHealthIndicator** - Auth services (rate limit, cache, blacklist)

**Health Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "version": "15.3",
        "active_connections": 8,
        "database_size": "245 MB",
        "response_time_ms": 12
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0",
        "used_memory_human": "1.24M",
        "connected_clients": "5"
      }
    },
    "authentication": {
      "status": "UP",
      "details": {
        "rate_limiting": "operational",
        "session_cache": "operational",
        "token_blacklist": "operational"
      }
    }
  },
  "system": {
    "memory": {
      "heap_used_mb": 512,
      "heap_max_mb": 2048
    },
    "threads": {
      "total": 45,
      "daemon": 38
    },
    "uptime_minutes": 1234
  }
}
```

### Monitoring Metrics

**Key Metrics to Track:**
- Login success rate (target: >99%)
- Login response time (target: <500ms p95)
- Failed login attempts (alert: >100/min)
- JWT validation time (target: <10ms)
- Redis hit rate (target: >95%)
- Token refresh rate
- Active sessions count
- API request rate per endpoint

**Prometheus Integration:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Sample Prometheus Queries:**
```promql
# Login success rate
rate(auth_login_success_total[5m]) / rate(auth_login_total[5m])

# Average response time
rate(http_server_requests_seconds_sum{uri="/api/v1/auth/login"}[5m])
  / rate(http_server_requests_seconds_count{uri="/api/v1/auth/login"}[5m])

# Failed login attempts
rate(auth_login_failed_total[5m])
```


---

## Testing

### Unit Tests

**Test JWT Token Provider:**
```java
@Test
void shouldGenerateValidAccessToken() {
    String token = jwtTokenProvider.generateToken("testuser", List.of("USER"));
    
    assertThat(token).isNotNull();
    assertThat(jwtTokenProvider.validateAccessToken(token)).isTrue();
    assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("testuser");
}

@Test
void shouldGenerateValidRefreshToken() {
    String token = jwtTokenProvider.generateRefreshToken("testuser");
    
    assertThat(token).isNotNull();
    assertThat(jwtTokenProvider.validateRefreshToken(token)).isTrue();
}
```

**Test Authentication Service:**
```java
@Test
void shouldRegisterUserSuccessfully() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("testuser");
    request.setEmail("test@example.com");
    request.setPassword("SecurePass123!");
    
    AuthenticationResponse response = authService.register(request, "127.0.0.1");
    
    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getRefreshToken()).isNotNull();
    assertThat(response.getUser().getUsername()).isEqualTo("testuser");
}
```

### Integration Tests

**Test Complete Login Flow:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {
    
    @Test
    void shouldLoginSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "usernameOrEmail": "test@example.com",
                        "password": "password"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }
}
```

### Load Testing

**Using Apache Bench:**
```bash
# Login endpoint - 1000 requests, 10 concurrent
ab -n 1000 -c 10 -p login.json -T application/json \
   http://localhost/api/v1/auth/login

# Expected results:
# - 99% of requests < 500ms
# - No failed requests
# - Load distributed across backends
```

**Using k6:**
```javascript
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  const payload = JSON.stringify({
    usernameOrEmail: 'test@example.com',
    password: 'password'
  });

  const res = http.post('http://localhost/api/v1/auth/login', payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'has access token': (r) => r.json('data.accessToken') !== null
  });
}
```


---

## Troubleshooting

### Common Issues

#### 1. "JWT secret must be configured"
**Cause:** JWT_SECRET environment variable not set  
**Solution:**
```bash
# Generate a secure secret
export JWT_SECRET=$(openssl rand -base64 64)

# Or set in .env file
echo "JWT_SECRET=$(openssl rand -base64 64)" >> .env
```

#### 2. "Too many login attempts"
**Cause:** Rate limit exceeded  
**Check:** Redis rate limit keys
```bash
redis-cli
> GET rate_limit:login:192.168.1.100
> TTL rate_limit:login:192.168.1.100
```
**Solution:** Wait for TTL to expire or manually delete key (dev only)

#### 3. "Account locked due to multiple failed login attempts"
**Cause:** 5+ failed login attempts  
**Check:** Redis failed login counter
```bash
redis-cli
> GET failed_login:user@example.com
> TTL failed_login:user@example.com
```
**Solution:** Wait 30 minutes or manually clear (admin only):
```bash
redis-cli DEL failed_login:user@example.com
```

#### 4. "Token has been revoked"
**Cause:** Token is blacklisted after logout  
**Check:** Redis blacklist
```bash
redis-cli
> EXISTS token:blacklist:{tokenId}
```
**Solution:** User must login again to get new tokens

#### 5. Backend instance not receiving requests
**Cause:** Health check failing  
**Check Nginx logs:**
```bash
docker logs ziboto-nginx | grep "upstream"
```
**Check backend health:**
```bash
curl http://spring-boot-1:8081/actuator/health
```
**Solution:** Fix backend health issue and Nginx will auto-recover

#### 6. "Could not get JDBC Connection"
**Cause:** Database connection pool exhausted  
**Check:**
```bash
# Monitor active connections
docker exec postgres psql -U ziboto -c \
  "SELECT count(*) FROM pg_stat_activity;"
```
**Solution:** Increase pool size or check for connection leaks

### Debug Mode

**Enable debug logging:**
```yaml
logging:
  level:
    com.ziboto.backend.auth: DEBUG
    com.ziboto.backend.security: DEBUG
    org.springframework.security: DEBUG
```

**Useful log patterns:**
```bash
# Login attempts
tail -f logs/ziboto.log | grep "Login attempt"

# Token validation
tail -f logs/ziboto.log | grep "JWT"

# Rate limiting
tail -f logs/ziboto.log | grep "Rate limit"

# Failed logins
tail -f logs/ziboto-security.log | grep "FAILED_LOGIN"
```


---

## Performance Benchmarks

### Expected Performance (Single Instance)

| Operation | Response Time (p50) | Response Time (p95) | Throughput |
|-----------|---------------------|---------------------|------------|
| Login | 150ms | 300ms | 100 req/s |
| Register | 200ms | 400ms | 50 req/s |
| Token Refresh | 80ms | 150ms | 200 req/s |
| Token Validation | 10ms | 20ms | 1000 req/s |
| Authenticated Request | 50ms | 100ms | 500 req/s |

### Scaling Performance (3 Instances)

| Metric | Single Instance | 3 Instances | Improvement |
|--------|----------------|-------------|-------------|
| Max Throughput | 100 req/s | 280 req/s | 2.8x |
| p95 Response Time | 300ms | 180ms | 40% faster |
| Concurrent Users | ~200 | ~600 | 3x |
| Fault Tolerance | None | 2 instances can fail | High |

### Bottleneck Analysis

**Most Common Bottlenecks:**
1. **Database Connections** - Increase pool size
2. **BCrypt Hashing** - CPU intensive, consider caching
3. **Redis Latency** - Use connection pooling
4. **Network I/O** - Use keep-alive connections

**Optimization Tips:**
- Enable Redis connection pooling (Lettuce)
- Use database connection pooling (HikariCP)
- Cache frequently accessed user data
- Enable Nginx keepalive to backends
- Use persistent connections

---

## Security Checklist

### Production Deployment

- [ ] **JWT Secret**: Use strong, randomly generated secret (64+ bytes)
- [ ] **HTTPS**: Enable SSL/TLS with valid certificates
- [ ] **Password Policy**: Enforce minimum requirements
- [ ] **Rate Limiting**: Configure appropriate limits per endpoint
- [ ] **Account Lockout**: Enable failed login tracking
- [ ] **Session Timeout**: Configure appropriate TTLs
- [ ] **Audit Logging**: Enable and monitor security events
- [ ] **Database**: Use strong passwords, enable encryption at rest
- [ ] **Redis**: Enable password authentication, disable dangerous commands
- [ ] **Firewall**: Restrict database and Redis to internal network only
- [ ] **Monitoring**: Set up alerts for suspicious activity
- [ ] **Backup**: Regular database backups
- [ ] **Updates**: Keep dependencies up to date

### Security Headers

Verify all security headers are present:
```bash
curl -I https://api.ziboto.com | grep -E "(Strict-Transport|X-Frame|X-Content-Type|X-XSS)"
```

Expected headers:
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
```


---

## Architecture Decisions

### Why JWT over Session-Based Auth?

**Advantages:**
- ✅ Stateless - No server-side session storage
- ✅ Horizontally scalable - No session replication needed
- ✅ Mobile-friendly - Works across platforms
- ✅ Performance - No database lookup per request
- ✅ Microservices - Easy to share across services

**Trade-offs:**
- ⚠️ Cannot revoke tokens easily (solved with blacklist)
- ⚠️ Token size larger than session ID (acceptable)
- ⚠️ Requires Redis for blacklist (already using Redis)

### Why Refresh Tokens?

**Benefits:**
- Short-lived access tokens (15 min) limit exposure window
- Long-lived refresh tokens (7 days) improve UX
- Token rotation on refresh improves security
- Can revoke specific refresh tokens per device

### Why BCrypt over Other Algorithms?

**Reasons:**
- Industry standard for password hashing
- Adaptive - can increase cost factor over time
- Built-in salt generation
- Resistant to rainbow tables
- Resistant to GPU attacks (memory-hard)

### Why Redis for Caching?

**Advantages:**
- In-memory speed for session data
- Built-in TTL support for expiry
- Atomic operations for counters
- Pub/sub for distributed events
- High availability with Sentinel

### Why Least-Connections Load Balancing?

**Better than Round-Robin because:**
- Accounts for varying request processing times
- Prevents overloading slow instances
- Better resource utilization
- Handles mixed workloads efficiently

---

## Future Enhancements

### Planned Features

1. **Multi-Factor Authentication (MFA)**
   - TOTP-based (Google Authenticator)
   - SMS-based OTP
   - Email-based OTP
   - Backup codes

2. **OAuth 2.0 / Social Login**
   - Google Sign-In
   - GitHub OAuth
   - Microsoft Azure AD
   - Facebook Login

3. **Advanced Security**
   - Device fingerprinting
   - Anomaly detection (unusual login locations)
   - Risk-based authentication
   - Password breach detection (HaveIBeenPwned)

4. **Session Management**
   - View active sessions per user
   - Revoke individual sessions
   - Logout from all devices
   - Device management (trusted devices)

5. **Role-Based Access Control (RBAC)**
   - Fine-grained permissions
   - Role hierarchy
   - Dynamic role assignment
   - Permission caching

6. **API Keys**
   - Generate API keys for programmatic access
   - Scope-based permissions
   - Rate limiting per key
   - Key rotation


---

## References

### Documentation
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [JWT Specification (RFC 7519)](https://datatracker.ietf.org/doc/html/rfc7519)
- [OAuth 2.0 Specification (RFC 6749)](https://datatracker.ietf.org/doc/html/rfc6749)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

### Dependencies
- **JJWT**: `io.jsonwebtoken:jjwt-api:0.12.3`
- **Spring Security**: `org.springframework.boot:spring-boot-starter-security`
- **Redis**: `org.springframework.boot:spring-boot-starter-data-redis`
- **BCrypt**: Included in Spring Security

### Related Documents
- `REDIS_ARCHITECTURE.md` - Redis caching strategy
- `SECURITY.md` - General security guidelines
- `nginx/README.md` - Nginx configuration guide
- `API_DOCUMENTATION.md` - Complete API reference

---

## Support & Contact

For issues or questions:
- **GitHub Issues**: [ziboto/backend/issues](https://github.com/ziboto/backend/issues)
- **Documentation**: [docs.ziboto.com](https://docs.ziboto.com)
- **Email**: support@ziboto.com

---

## Changelog

### Version 1.0.0 (2026-08-05)
- ✅ Initial implementation
- ✅ JWT authentication with access and refresh tokens
- ✅ Redis caching for sessions and rate limiting
- ✅ Nginx load balancing configuration
- ✅ Horizontal scaling support
- ✅ Health check endpoints
- ✅ Comprehensive audit logging
- ✅ BCrypt password hashing
- ✅ Account lockout on failed attempts
- ✅ Token blacklisting on logout

---

**Document Version:** 1.0.0  
**Last Updated:** August 5, 2026  
**Author:** Ziboto Development Team  
**Status:** ✅ Production Ready
