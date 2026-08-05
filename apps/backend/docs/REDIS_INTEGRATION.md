# Redis Integration Guide

## Overview

The Ziboto backend uses Redis for high-performance caching, rate limiting, session management, and token blacklisting. All Redis functionality is centralized, configurable, and production-ready.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
├─────────────────────────────────────────────────────────────┤
│  RateLimitService  │  FailedLoginAttemptService  │  etc...  │
├─────────────────────────────────────────────────────────────┤
│                      RedisService                            │
│              (Centralized Redis Operations)                  │
├─────────────────────────────────────────────────────────────┤
│                    RedisTemplate                             │
│              (Spring Data Redis)                             │
├─────────────────────────────────────────────────────────────┤
│                       Redis Server                           │
└─────────────────────────────────────────────────────────────┘
```

## Components

### 1. RedisService
**Location:** `com.ziboto.backend.cache.RedisService`

Centralized service providing all Redis operations:
- **Key-Value Operations:** set, get, delete with TTL support
- **Counter Operations:** increment, decrement with automatic TTL
- **Hash Operations:** store and retrieve hash maps
- **Set Operations:** manage sets of values
- **TTL Management:** set, get, and check expiration

**Usage Example:**
```java
@Autowired
private RedisService redisService;

// Simple caching
redisService.set("user:123", userData, Duration.ofHours(1));
UserData cached = redisService.get("user:123", UserData.class);

// Rate limiting counter
Long count = redisService.incrementWithTTL("rate:login:user123", Duration.ofMinutes(15));
boolean limited = count != null && count > 5;

// Hash operations for session metadata
redisService.hashSet("session:abc123", "ip", "192.168.1.1");
Map<String, Object> session = redisService.hashGetAll("session:abc123");
```

### 2. RedisProperties
**Location:** `com.ziboto.backend.config.properties.RedisProperties`

Centralized configuration for all Redis-related settings:
- Rate limiting (login, signup, API, token refresh)
- Failed login attempt tracking
- Session cache settings
- Token blacklist configuration
- OTP settings
- Key namespace prefixes

### 3. Rate Limiting
**Service:** `RateLimitService`

Protects against brute force and abuse:
- **Login Rate Limiting:** Limit login attempts per user/IP
- **Signup Rate Limiting:** Prevent spam registrations
- **API Rate Limiting:** Limit API requests per user
- **Token Refresh Limiting:** Prevent token refresh abuse

**Configuration:**
```yaml
app:
  redis:
    rate-limit:
      login:
        max-attempts: 5
        window-minutes: 15
      signup:
        max-attempts: 3
        window-minutes: 60
      api:
        max-requests: 100
        window-minutes: 1
      refresh:
        max-attempts: 10
        window-hours: 1
```

**Usage:**
```java
@Autowired
private RateLimitService rateLimitService;

// Check and record login attempt
if (rateLimitService.isLoginRateLimitExceeded(username)) {
    throw new RateLimitExceededException("Too many login attempts");
}
int remaining = rateLimitService.recordLoginAttempt(username);

// Reset after successful login
rateLimitService.resetLoginRateLimit(username);
```

### 4. Failed Login Tracking
**Service:** `FailedLoginAttemptService`

Tracks failed login attempts and implements account lockout:
- Automatic account lockout after threshold
- Configurable lockout duration
- Automatic unlock after cooldown
- Track attempt timestamps

**Configuration:**
```yaml
app:
  redis:
    failed-login:
      max-attempts: 5
      lockout-minutes: 30
      tracking-hours: 1
```

**Usage:**
```java
@Autowired
private FailedLoginAttemptService failedLoginService;

// Check if account is locked
if (failedLoginService.isLocked(username)) {
    long remainingTime = failedLoginService.getLockoutRemainingTime(username);
    throw new AccountLockedException("Account locked for " + remainingTime + " seconds");
}

// Record failed attempt
failedLoginService.recordFailedAttempt(username);

// Reset after successful login
failedLoginService.resetFailedAttempts(username);
```

### 5. Session Cache
**Service:** `SessionCacheService`

Caches user session data to reduce database load:
- User profile caching with TTL
- Session metadata (IP, device, timestamps)
- Active session tracking
- Concurrent session limiting
- Optional sliding window expiration

**Configuration:**
```yaml
app:
  redis:
    session:
      ttl-hours: 1
      extended-ttl-hours: 24
      sliding-window: true
      max-concurrent-sessions: 0  # 0 = unlimited
```

**Usage:**
```java
@Autowired
private SessionCacheService sessionCacheService;

// Cache user session
sessionCacheService.cacheUserSession(username, userResponse);

// Get cached session
UserResponse cached = sessionCacheService.getCachedUserSession(username);

// Track active session
sessionCacheService.trackActiveSession(username, sessionId, deviceInfo);

// Invalidate on logout
sessionCacheService.invalidateUserSession(username);
```

### 6. Token Blacklist
**Service:** `TokenBlacklistService`

Implements JWT token revocation:
- Blacklist individual tokens after logout
- Blacklist all user tokens (logout all devices)
- Automatic expiration based on token TTL
- Support for password change scenarios

**Configuration:**
```yaml
app:
  redis:
    token-blacklist:
      enabled: true
      max-ttl-days: 7
```

**Usage:**
```java
@Autowired
private TokenBlacklistService tokenBlacklistService;

// Blacklist token on logout
tokenBlacklistService.blacklistToken(token);

// Check if token is blacklisted
if (tokenBlacklistService.isTokenBlacklisted(token)) {
    throw new InvalidTokenException("Token has been revoked");
}

// Blacklist all user tokens (logout all devices)
tokenBlacklistService.blacklistAllUserTokens(username);
```

### 7. OTP Cache (Future Use)
**Service:** `OtpCacheService`

Manages One-Time Passwords for verification flows:
- Secure OTP generation (4-8 digits)
- Configurable TTL (default 5 minutes)
- Rate limiting on OTP generation
- Verification attempt tracking
- Support for multiple OTP purposes (email, phone, 2FA, etc.)

**Configuration:**
```yaml
app:
  redis:
    otp:
      ttl-minutes: 5
      max-attempts: 3
      rate-limit-minutes: 15
      max-verification-attempts: 3
```

**Usage:**
```java
@Autowired
private OtpCacheService otpCacheService;

// Generate OTP
String otp = otpCacheService.generateOtp(email, OtpPurpose.EMAIL_VERIFICATION);

// Verify OTP
boolean valid = otpCacheService.verifyOtp(email, userProvidedOtp, OtpPurpose.EMAIL_VERIFICATION);

// Verify and invalidate
boolean valid = otpCacheService.verifyAndInvalidateOtp(email, otp, OtpPurpose.EMAIL_VERIFICATION);
```

## Configuration

### Environment Variables

All Redis settings can be configured via environment variables:

```bash
# Redis Connection
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# Rate Limiting
REDIS_RATE_LIMIT_LOGIN_MAX=5
REDIS_RATE_LIMIT_LOGIN_WINDOW=15
REDIS_RATE_LIMIT_SIGNUP_MAX=3
REDIS_RATE_LIMIT_SIGNUP_WINDOW=60
REDIS_RATE_LIMIT_API_MAX=100
REDIS_RATE_LIMIT_API_WINDOW=1
REDIS_RATE_LIMIT_REFRESH_MAX=10
REDIS_RATE_LIMIT_REFRESH_WINDOW=1

# Failed Login Tracking
REDIS_FAILED_LOGIN_MAX=5
REDIS_FAILED_LOGIN_LOCKOUT=30
REDIS_FAILED_LOGIN_TRACKING=1

# Session Cache
REDIS_SESSION_TTL=1
REDIS_SESSION_EXTENDED_TTL=24
REDIS_SESSION_SLIDING_WINDOW=true
REDIS_SESSION_MAX_CONCURRENT=0

# Token Blacklist
REDIS_TOKEN_BLACKLIST_ENABLED=true
REDIS_TOKEN_BLACKLIST_MAX_TTL=7

# OTP
REDIS_OTP_TTL=5
REDIS_OTP_MAX_ATTEMPTS=3
REDIS_OTP_RATE_LIMIT=15
REDIS_OTP_MAX_VERIFY=3
```

### Application YAML

Configuration can also be set in `application.yml`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 60000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms

app:
  redis:
    rate-limit:
      login:
        max-attempts: 5
        window-minutes: 15
    # ... (see full configuration in application.yml)
```

## Redis Key Namespaces

All Redis keys use namespaced prefixes to prevent conflicts:

- **Rate Limiting:** `rate_limit:{type}:{identifier}`
  - Example: `rate_limit:login:john@example.com`
  
- **Failed Login:** `failed_login:{type}:{identifier}`
  - Example: `failed_login:attempts:john@example.com`
  
- **Session:** `session:{type}:{identifier}`
  - Example: `session:user:john@example.com`
  
- **Token Blacklist:** `token:blacklist:{type}:{identifier}`
  - Example: `token:blacklist:token:eyJhbGc...`
  
- **OTP:** `otp:{type}:{purpose}:{identifier}`
  - Example: `otp:otp:email_verification:john@example.com`

## Production Best Practices

### 1. Connection Pooling
The application uses Lettuce connection pool with optimized settings:
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### 2. Fail-Open Strategy
All Redis operations fail gracefully. Redis failures are logged but don't break the application flow.

### 3. TTL Management
All cached data has appropriate TTL values to prevent memory leaks:
- Rate limit counters: Auto-expire with configured window
- Session data: 1 hour (default, configurable)
- Blacklisted tokens: Expire with token TTL
- Failed login tracking: 1 hour (configurable)

### 4. Key Expiration
Redis automatically removes expired keys, ensuring efficient memory usage.

### 5. Monitoring
Use Redis monitoring tools to track:
- Memory usage
- Key count
- Hit/miss rates
- Connection pool stats

## Deployment Considerations

### Docker Compose
```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

volumes:
  redis-data:
```

### Redis Sentinel (High Availability)
For production, consider Redis Sentinel for automatic failover:
```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - sentinel1:26379
          - sentinel2:26379
          - sentinel3:26379
```

### Redis Cluster (Scalability)
For high-scale deployments, use Redis Cluster:
```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - node1:6379
          - node2:6379
          - node3:6379
```

## Testing

### Local Redis with Docker
```bash
docker run -d --name ziboto-redis -p 6379:6379 redis:7-alpine
```

### Redis CLI Testing
```bash
# Connect to Redis
redis-cli

# Monitor all commands
MONITOR

# Check keys
KEYS rate_limit:*
KEYS session:*

# Check TTL
TTL rate_limit:login:john@example.com

# Get value
GET session:user:john@example.com

# Delete keys (testing only)
DEL rate_limit:login:john@example.com
FLUSHDB  # Clear entire database (careful!)
```

## Troubleshooting

### Connection Issues
```
Error: Unable to connect to Redis at localhost:6379
```
**Solution:** Check Redis is running and port is correct:
```bash
docker ps  # Check if Redis container is running
redis-cli ping  # Should return PONG
```

### Memory Issues
```
Error: OOM command not allowed when used memory > 'maxmemory'
```
**Solution:** Configure Redis maxmemory and eviction policy:
```
maxmemory 256mb
maxmemory-policy allkeys-lru
```

### Performance Issues
- Monitor slow queries: `redis-cli --latency`
- Check connected clients: `CLIENT LIST`
- Monitor memory: `INFO memory`
- Check key count: `DBSIZE`

## Migration from In-Memory to Redis

If migrating from in-memory caching:

1. Update service dependencies to inject `RedisService`
2. Replace direct Redis Template calls with `RedisService` methods
3. Update configuration to use `RedisProperties`
4. Test thoroughly in staging environment
5. Monitor Redis metrics after deployment

## Security Considerations

1. **Authentication:** Always set Redis password in production
2. **Network Security:** Use firewall rules to restrict Redis access
3. **Encryption:** Consider Redis TLS for data in transit
4. **Key Naming:** Use namespaced keys to prevent conflicts
5. **Data Sensitivity:** Don't cache sensitive data without encryption

## Performance Metrics

Expected performance characteristics:
- **Get Operation:** < 1ms
- **Set Operation:** < 1ms  
- **Increment:** < 1ms
- **Hash Operations:** < 2ms
- **Throughput:** 100K+ ops/sec (single instance)

## Additional Resources

- [Spring Data Redis Documentation](https://spring.io/projects/spring-data-redis)
- [Redis Documentation](https://redis.io/documentation)
- [Redis Best Practices](https://redis.io/topics/best-practices)
- [Lettuce Documentation](https://lettuce.io/)
