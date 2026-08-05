# Redis Integration for Authentication

This document describes the Redis integration for authentication services in Ziboto.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Key Patterns](#key-patterns)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)
- [Monitoring](#monitoring)

---

## Overview

Ziboto uses Redis for high-performance authentication caching and security features:

- ✅ **Rate Limiting** - Prevent brute force attacks
- ✅ **Failed Login Tracking** - Account lockout after threshold
- ✅ **Session Caching** - Reduce database queries
- ✅ **Token Blacklisting** - JWT revocation for logout
- ✅ **Automatic Expiration** - TTL-based cleanup

### Benefits

1. **Performance** - Sub-millisecond response times
2. **Scalability** - Distributed caching across instances
3. **Security** - Real-time threat detection and blocking
4. **Reliability** - Automatic failover and persistence
5. **Simplicity** - No complex distributed state management

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Application Layer                         │
├─────────────────────────────────────────────────────────────────┤
│  AuthController → AuthService → Redis Services                   │
│                                                                   │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │ RateLimit      │  │ FailedLogin    │  │ TokenBlacklist   │  │
│  │ Service        │  │ AttemptService │  │ Service          │  │
│  └────────────────┘  └────────────────┘  └──────────────────┘  │
│                                                                   │
│  ┌────────────────┐  ┌────────────────┐                         │
│  │ SessionCache   │  │ Cache          │                         │
│  │ Service        │  │ Service        │                         │
│  └────────────────┘  └────────────────┘                         │
├─────────────────────────────────────────────────────────────────┤
│                         Redis Layer                              │
├─────────────────────────────────────────────────────────────────┤
│  RedisTemplate<String, Object> ← RedisConfig                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Services

### 1. CacheService

**Purpose:** General-purpose Redis caching

**Location:** `cache/CacheService.java`

**Features:**
- Get/Set operations with serialization
- TTL management
- Pattern-based deletion
- Key existence checks
- Increment/Decrement operations

**Methods:**
```java
void set(String key, Object value)
void set(String key, Object value, Duration ttl)
Object get(String key)
<T> T get(String key, Class<T> type)
boolean delete(String key)
long deleteByPattern(String pattern)
boolean exists(String key)
boolean expire(String key, Duration ttl)
Long increment(String key)
Long decrement(String key)
```

**Usage:**
```java
@Autowired
private CacheService cacheService;

// Cache user data
cacheService.set("user:123", user, Duration.ofHours(1));

// Retrieve cached data
User user = cacheService.get("user:123", User.class);

// Delete cache
cacheService.delete("user:123");
```

---

### 2. RateLimitService

**Purpose:** Rate limiting to prevent abuse

**Location:** `auth/service/RateLimitService.java`

**Configuration:**
| Type | Max Attempts | Window |
|------|--------------|--------|
| Login | 5 | 15 minutes |
| API | 100 | 1 minute |
| Refresh | 10 | 1 hour |

**Methods:**
```java
// Login rate limiting
boolean isLoginRateLimitExceeded(String identifier)
int recordLoginAttempt(String identifier)
int getRemainingLoginAttempts(String identifier)
void resetLoginRateLimit(String identifier)
long getLoginRateLimitResetTime(String identifier)

// API rate limiting
boolean isApiRateLimitExceeded(Long userId)
int recordApiRequest(Long userId)

// Token refresh rate limiting
boolean isRefreshRateLimitExceeded(Long userId)
int recordRefreshAttempt(Long userId)
```

**Usage:**
```java
@Autowired
private RateLimitService rateLimitService;

// Check login rate limit
if (rateLimitService.isLoginRateLimitExceeded(username)) {
    long resetTime = rateLimitService.getLoginRateLimitResetTime(username);
    throw new RateLimitExceededException("Too many attempts. Try again in " + resetTime + "s");
}

// Record attempt
int remaining = rateLimitService.recordLoginAttempt(username);

// Reset after successful login
rateLimitService.resetLoginRateLimit(username);
```

---

### 3. FailedLoginAttemptService

**Purpose:** Track failed login attempts and lock accounts

**Location:** `auth/service/FailedLoginAttemptService.java`

**Configuration:**
- Max failed attempts: 5
- Lockout duration: 30 minutes
- Tracking window: 1 hour

**Methods:**
```java
void recordFailedAttempt(String identifier)
boolean isLocked(String identifier)
int getFailedAttempts(String identifier)
int getRemainingAttempts(String identifier)
long getLockoutRemainingTime(String identifier)
void resetFailedAttempts(String identifier)
void unlockAccount(String identifier)
String getLastFailedAttemptTime(String identifier)
boolean shouldLock(String identifier)
```

**Usage:**
```java
@Autowired
private FailedLoginAttemptService failedLoginService;

// Check if account is locked
if (failedLoginService.isLocked(username)) {
    long remaining = failedLoginService.getLockoutRemainingTime(username);
    throw new AccountLockedException("Account locked. Unlocks in " + remaining + "s");
}

// Record failed attempt
failedLoginService.recordFailedAttempt(username);

// Check remaining attempts
int remaining = failedLoginService.getRemainingAttempts(username);
log.warn("Failed login for: {} - {} attempts remaining", username, remaining);

// Reset after successful login
failedLoginService.resetFailedAttempts(username);
```

---

### 4. TokenBlacklistService

**Purpose:** JWT token revocation and blacklisting

**Location:** `auth/service/TokenBlacklistService.java`

**Features:**
- Blacklist individual tokens (logout)
- Blacklist all user tokens (logout all devices)
- Blacklist tokens before timestamp (password change)
- Automatic expiration based on token TTL

**Methods:**
```java
void blacklistToken(String token)
boolean isTokenBlacklisted(String token)
void blacklistUserTokensBefore(String username, Date beforeTime)
void blacklistAllUserTokens(String username)
void removeFromBlacklist(String token)
void clearUserBlacklist(String username)
long getUserBlacklistRemainingTime(String username)
boolean hasUserBlacklist(String username)
```

**Usage:**
```java
@Autowired
private TokenBlacklistService tokenBlacklistService;

// Logout - blacklist token
tokenBlacklistService.blacklistToken(accessToken);

// Logout all devices
tokenBlacklistService.blacklistAllUserTokens(username);

// After password change
tokenBlacklistService.blacklistUserTokensBefore(username, new Date());

// Check if token is blacklisted (in JwtAuthenticationFilter)
if (tokenBlacklistService.isTokenBlacklisted(token)) {
    throw new InvalidTokenException("Token has been revoked");
}
```

---

### 5. SessionCacheService

**Purpose:** Cache user session data and metadata

**Location:** `auth/service/SessionCacheService.java`

**Features:**
- User profile caching
- Session metadata storage
- Active session tracking
- Sliding window expiration

**TTL Configuration:**
- Default session: 1 hour
- Extended session: 24 hours

**Methods:**
```java
// User session caching
void cacheUserSession(String username, UserResponse userResponse)
void cacheUserSession(String username, UserResponse userResponse, Duration ttl)
UserResponse getCachedUserSession(String username)
void invalidateUserSession(String username)
void refreshSessionTTL(String username)

// Session metadata
void cacheSessionMetadata(String sessionId, Map<String, Object> metadata)
Map<String, Object> getSessionMetadata(String sessionId)
void updateSessionMetadata(String sessionId, String field, Object value)
void deleteSessionMetadata(String sessionId)

// Active session tracking
void trackActiveSession(String username, String sessionId, String deviceInfo)
Map<String, Object> getActiveSessions(String username)
void removeActiveSession(String username, String sessionId)
void clearAllActiveSessions(String username)
long countActiveSessions(String username)
boolean isSessionActive(String username, String sessionId)
```

**Usage:**
```java
@Autowired
private SessionCacheService sessionCacheService;

// Cache user session
sessionCacheService.cacheUserSession(username, userResponse);

// Get cached session (reduces DB queries)
UserResponse cached = sessionCacheService.getCachedUserSession(username);
if (cached != null) {
    return cached; // Fast path
}

// Invalidate cache after profile update
sessionCacheService.invalidateUserSession(username);

// Track active session
Map<String, Object> metadata = Map.of(
    "ipAddress", "192.168.1.1",
    "userAgent", "Mozilla/5.0...",
    "loginTime", LocalDateTime.now()
);
sessionCacheService.cacheSessionMetadata(sessionId, metadata);
sessionCacheService.trackActiveSession(username, sessionId, deviceInfo);

// Get all active sessions
Map<String, Object> sessions = sessionCacheService.getActiveSessions(username);
```

---

## Key Patterns

Redis keys follow a hierarchical naming convention:

| Pattern | Description | Example |
|---------|-------------|---------|
| `rate_limit:login:{id}` | Login rate limit counter | `rate_limit:login:john_doe` |
| `rate_limit:api:{userId}` | API rate limit counter | `rate_limit:api:123` |
| `rate_limit:refresh:{userId}` | Refresh rate limit | `rate_limit:refresh:123` |
| `failed_login:attempts:{id}` | Failed login counter | `failed_login:attempts:john_doe` |
| `failed_login:lockout:{id}` | Account lockout flag | `failed_login:lockout:john_doe` |
| `failed_login:last:{id}` | Last failed attempt time | `failed_login:last:john_doe` |
| `token:blacklist:{token}` | Individual token blacklist | `token:blacklist:eyJ...` |
| `token:user_blacklist:{user}` | User token blacklist | `token:user_blacklist:john_doe` |
| `session:user:{username}` | User session cache | `session:user:john_doe` |
| `session:meta:{sessionId}` | Session metadata | `session:meta:uuid-1234` |
| `session:active:{username}` | Active sessions hash | `session:active:john_doe` |
| `ziboto:{cacheName}:{key}` | General cache | `ziboto:users:123` |

---

## Configuration

### application.yml

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
  cache:
    ttl: ${CACHE_TTL:3600}    # 1 hour in seconds
    prefix: "ziboto"
```

### Environment Variables

```bash
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
REDIS_DATABASE=0

# Cache Configuration
CACHE_TTL=3600
```

### Docker Compose

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --requirepass your-redis-password
    volumes:
      - redis-data:/data
    restart: unless-stopped

volumes:
  redis-data:
```

---

## Usage Examples

### Complete Login Flow with Redis

```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;
    private final FailedLoginAttemptService failedLoginService;
    private final SessionCacheService sessionCacheService;
    
    @Override
    public AuthenticationResponse login(LoginRequest request, String ipAddress) {
        String identifier = request.getUsernameOrEmail();
        
        // 1. Check rate limit
        if (rateLimitService.isLoginRateLimitExceeded(identifier)) {
            long resetTime = rateLimitService.getLoginRateLimitResetTime(identifier);
            throw new RateLimitExceededException(
                "Too many login attempts. Try again in " + resetTime + " seconds"
            );
        }
        
        // 2. Check if account is locked
        if (failedLoginService.isLocked(identifier)) {
            long unlockTime = failedLoginService.getLockoutRemainingTime(identifier);
            throw new AccountLockedException(
                "Account is locked. Unlocks in " + unlockTime + " seconds"
            );
        }
        
        // 3. Record rate limit attempt
        rateLimitService.recordLoginAttempt(identifier);
        
        try {
            // 4. Authenticate
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsernameOrEmail(),
                    request.getPassword()
                )
            );
            
            // 5. Generate tokens
            String accessToken = tokenProvider.generateToken(auth);
            String refreshToken = tokenProvider.generateRefreshToken(auth);
            
            // 6. Load user
            User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow();
            UserResponse userResponse = userMapper.toUserResponse(user);
            
            // 7. Cache user session
            sessionCacheService.cacheUserSession(user.getUsername(), userResponse);
            
            // 8. Track active session
            String sessionId = UUID.randomUUID().toString();
            Map<String, Object> metadata = Map.of(
                "ipAddress", ipAddress,
                "loginTime", LocalDateTime.now()
            );
            sessionCacheService.cacheSessionMetadata(sessionId, metadata);
            sessionCacheService.trackActiveSession(
                user.getUsername(),
                sessionId,
                "Web Browser"
            );
            
            // 9. Reset security counters on success
            rateLimitService.resetLoginRateLimit(identifier);
            failedLoginService.resetFailedAttempts(identifier);
            
            return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .build();
                
        } catch (AuthenticationException e) {
            // 10. Record failed attempt
            failedLoginService.recordFailedAttempt(identifier);
            int remaining = failedLoginService.getRemainingAttempts(identifier);
            
            throw new BadCredentialsException(
                "Invalid credentials. " + remaining + " attempts remaining"
            );
        }
    }
}
```

### Logout with Token Blacklisting

```java
@Override
public void logout(String token, String username) {
    // 1. Blacklist the token
    tokenBlacklistService.blacklistToken(token);
    
    // 2. Invalidate session cache
    sessionCacheService.invalidateUserSession(username);
    
    // 3. Remove active session
    String sessionId = extractSessionIdFromToken(token);
    sessionCacheService.removeActiveSession(username, sessionId);
    sessionCacheService.deleteSessionMetadata(sessionId);
    
    log.info("User logged out: {}", username);
}
```

### Logout All Devices

```java
@Override
public void logoutAllDevices(String username) {
    // 1. Blacklist all user tokens
    tokenBlacklistService.blacklistAllUserTokens(username);
    
    // 2. Clear session cache
    sessionCacheService.invalidateUserSession(username);
    
    // 3. Clear all active sessions
    sessionCacheService.clearAllActiveSessions(username);
    
    log.info("Logged out all devices for user: {}", username);
}
```

### Token Validation with Blacklist Check

```java
// In JwtAuthenticationFilter
if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
    
    // Check if token is blacklisted
    if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
        log.warn("Attempted use of blacklisted token");
        return; // Don't authenticate
    }
    
    String username = tokenProvider.getUsernameFromToken(jwt);
    
    // Try to get from cache first
    UserDetails userDetails = sessionCacheService.getCachedUserSession(username);
    if (userDetails == null) {
        // Cache miss - load from database
        userDetails = userDetailsService.loadUserByUsername(username);
        sessionCacheService.cacheUserSession(username, userDetails);
    }
    
    // Set authentication...
}
```

---

## Monitoring

### Redis CLI Commands

```bash
# Connect to Redis
redis-cli -h localhost -p 6379 -a your-password

# Monitor keys
KEYS rate_limit:login:*
KEYS token:blacklist:*
KEYS session:user:*

# Check specific key
GET rate_limit:login:john_doe
TTL rate_limit:login:john_doe

# Clear all rate limits
KEYS rate_limit:* | xargs redis-cli DEL

# Monitor real-time commands
MONITOR
```

### Key Metrics to Track

1. **Rate Limit Hits**
   - Login rate limit violations
   - API rate limit violations
   - Trend analysis

2. **Failed Login Attempts**
   - Failed attempts per user
   - Account lockouts
   - Geographic distribution

3. **Token Blacklist**
   - Blacklisted tokens count
   - User logouts per day
   - "Logout all devices" events

4. **Session Cache**
   - Cache hit ratio
   - Average session duration
   - Active sessions count

5. **Redis Performance**
   - Memory usage
   - Hit/miss ratio
   - Command latency

---

## Best Practices

### ✅ DO

- ✅ Use appropriate TTLs for all keys
- ✅ Namespace keys with prefixes
- ✅ Handle Redis failures gracefully (fail open for non-critical features)
- ✅ Monitor Redis memory usage
- ✅ Use connection pooling
- ✅ Log security events
- ✅ Implement circuit breakers for Redis calls
- ✅ Use Redis persistence (AOF/RDB)

### ❌ DON'T

- ❌ Store sensitive data without encryption
- ❌ Use blocking operations in request path
- ❌ Forget to set TTLs (causes memory leaks)
- ❌ Use KEYS command in production (use SCAN)
- ❌ Store large objects in Redis
- ❌ Hard-fail on Redis errors (affects availability)

---

## Troubleshooting

### Issue: Rate limit not working

**Symptoms:** Users can make unlimited login attempts

**Solutions:**
1. Check Redis connection
2. Verify REDIS_HOST and REDIS_PORT
3. Check Redis auth password
4. Review rate limit configuration

### Issue: Sessions not caching

**Symptoms:** High database load despite Redis

**Solutions:**
1. Check cache hit/miss ratio
2. Verify serialization configuration
3. Check TTL settings
4. Review cache invalidation logic

### Issue: Tokens not blacklisting

**Symptoms:** Logged out users can still access API

**Solutions:**
1. Verify TokenBlacklistService is being called
2. Check token TTL calculation
3. Ensure JwtAuthenticationFilter checks blacklist
4. Review Redis key expiration

---

## References

- [Redis Documentation](https://redis.io/documentation)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Rate Limiting Algorithms](https://en.wikipedia.org/wiki/Rate_limiting)
- [JWT Token Revocation](https://auth0.com/blog/blacklist-json-web-token-api-keys/)
