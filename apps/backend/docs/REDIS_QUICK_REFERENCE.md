# Redis Quick Reference

## Quick Start

### 1. Start Redis (Docker)
```bash
docker run -d --name ziboto-redis -p 6379:6379 redis:7-alpine
```

### 2. Configure Environment
```bash
# .env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

### 3. Use in Code
```java
@Autowired
private RedisService redisService;

// Set with TTL
redisService.set("key", value, Duration.ofMinutes(5));

// Get
Object value = redisService.get("key");
```

## Common Patterns

### Rate Limiting
```java
@Autowired
private RateLimitService rateLimitService;

// Check limit
if (rateLimitService.isLoginRateLimitExceeded(username)) {
    throw new RateLimitExceededException();
}

// Record attempt
int remaining = rateLimitService.recordLoginAttempt(username);

// Reset on success
rateLimitService.resetLoginRateLimit(username);
```

### Failed Login Tracking
```java
@Autowired
private FailedLoginAttemptService failedLoginService;

// Check if locked
if (failedLoginService.isLocked(username)) {
    long seconds = failedLoginService.getLockoutRemainingTime(username);
    throw new AccountLockedException("Locked for " + seconds + "s");
}

// Record failure
failedLoginService.recordFailedAttempt(username);

// Reset on success
failedLoginService.resetFailedAttempts(username);
```

### Session Caching
```java
@Autowired
private SessionCacheService sessionCache;

// Cache session
sessionCache.cacheUserSession(username, userResponse);

// Get cached
UserResponse user = sessionCache.getCachedUserSession(username);

// Invalidate
sessionCache.invalidateUserSession(username);
```

### Token Blacklisting
```java
@Autowired
private TokenBlacklistService tokenBlacklist;

// Blacklist on logout
tokenBlacklist.blacklistToken(token);

// Check if blacklisted
if (tokenBlacklist.isTokenBlacklisted(token)) {
    throw new InvalidTokenException();
}

// Blacklist all user tokens
tokenBlacklist.blacklistAllUserTokens(username);
```

### OTP Generation & Verification
```java
@Autowired
private OtpCacheService otpCache;

// Generate OTP
String otp = otpCache.generateOtp(
    email, 
    OtpPurpose.EMAIL_VERIFICATION
);

// Verify OTP
boolean valid = otpCache.verifyOtp(
    email, 
    userOtp, 
    OtpPurpose.EMAIL_VERIFICATION
);

// Verify and invalidate (one-time use)
boolean valid = otpCache.verifyAndInvalidateOtp(
    email, 
    otp, 
    OtpPurpose.EMAIL_VERIFICATION
);
```

## Configuration Cheat Sheet

### Rate Limits (Minutes/Hours)
```yaml
# Login: 5 attempts per 15 minutes
REDIS_RATE_LIMIT_LOGIN_MAX=5
REDIS_RATE_LIMIT_LOGIN_WINDOW=15

# Signup: 3 attempts per 60 minutes
REDIS_RATE_LIMIT_SIGNUP_MAX=3
REDIS_RATE_LIMIT_SIGNUP_WINDOW=60

# API: 100 requests per minute
REDIS_RATE_LIMIT_API_MAX=100
REDIS_RATE_LIMIT_API_WINDOW=1

# Refresh: 10 attempts per hour
REDIS_RATE_LIMIT_REFRESH_MAX=10
REDIS_RATE_LIMIT_REFRESH_WINDOW=1
```

### Failed Login Settings
```yaml
# 5 failed attempts trigger lockout
REDIS_FAILED_LOGIN_MAX=5

# Locked for 30 minutes
REDIS_FAILED_LOGIN_LOCKOUT=30

# Track attempts for 1 hour
REDIS_FAILED_LOGIN_TRACKING=1
```

### Session Cache
```yaml
# Cache for 1 hour
REDIS_SESSION_TTL=1

# Metadata for 24 hours
REDIS_SESSION_EXTENDED_TTL=24

# Refresh on access
REDIS_SESSION_SLIDING_WINDOW=true

# No concurrent session limit
REDIS_SESSION_MAX_CONCURRENT=0
```

### OTP Settings
```yaml
# OTP valid for 5 minutes
REDIS_OTP_TTL=5

# Max 3 OTP generations per 15 minutes
REDIS_OTP_MAX_ATTEMPTS=3
REDIS_OTP_RATE_LIMIT=15

# Max 3 verification attempts
REDIS_OTP_MAX_VERIFY=3
```

## Redis CLI Commands

### Monitor Activity
```bash
redis-cli MONITOR
```

### Check Keys
```bash
# List all rate limit keys
redis-cli KEYS "rate_limit:*"

# List all session keys
redis-cli KEYS "session:*"

# Count all keys
redis-cli DBSIZE
```

### Inspect Values
```bash
# Get value
redis-cli GET "session:user:john"

# Get TTL
redis-cli TTL "rate_limit:login:john"

# Get hash
redis-cli HGETALL "session:meta:abc123"
```

### Manual Operations
```bash
# Delete key
redis-cli DEL "rate_limit:login:john"

# Reset rate limit
redis-cli DEL "rate_limit:login:*"

# Clear database (careful!)
redis-cli FLUSHDB
```

## Key Patterns

```
rate_limit:{type}:{identifier}
  Examples:
  - rate_limit:login:john@example.com
  - rate_limit:signup:192.168.1.1
  - rate_limit:api:12345
  - rate_limit:refresh:12345

failed_login:{type}:{identifier}
  Examples:
  - failed_login:attempts:john@example.com
  - failed_login:lockout:john@example.com
  - failed_login:last:john@example.com

session:{type}:{identifier}
  Examples:
  - session:user:john@example.com
  - session:meta:token-id-123
  - session:active:john@example.com

token:blacklist:{type}:{identifier}
  Examples:
  - token:blacklist:token:eyJhbG...
  - token:blacklist:user:john@example.com

otp:{type}:{purpose}:{identifier}
  Examples:
  - otp:otp:email_verification:john@example.com
  - otp:meta:email_verification:john@example.com
  - otp:rate:email_verification:john@example.com
```

## Troubleshooting

### Connection Failed
```bash
# Check Redis is running
docker ps | grep redis

# Test connection
redis-cli ping
# Expected: PONG
```

### Out of Memory
```bash
# Check memory usage
redis-cli INFO memory

# Check eviction policy
redis-cli CONFIG GET maxmemory-policy
```

### Slow Performance
```bash
# Check for slow queries
redis-cli SLOWLOG GET 10

# Monitor latency
redis-cli --latency

# Check connected clients
redis-cli CLIENT LIST
```

### Clear All Data
```bash
# Development only!
redis-cli FLUSHALL
```

## Testing Checklist

- [ ] Redis connection established
- [ ] Rate limiting enforced
- [ ] Failed login lockout works
- [ ] Session caching functional
- [ ] Token blacklist effective
- [ ] OTP generation working
- [ ] All TTLs expire correctly
- [ ] Configuration changes applied

## Performance Tips

1. **Use pipelining** for multiple operations
2. **Set appropriate TTLs** on all keys
3. **Monitor memory usage** regularly
4. **Use namespaced keys** to avoid conflicts
5. **Configure connection pool** properly
6. **Enable persistence** for production
7. **Use Redis Sentinel** for HA
8. **Monitor slow queries** and optimize

## Common Errors

### "Unable to connect to Redis"
- Check Redis is running
- Verify host/port configuration
- Check firewall rules

### "OOM command not allowed"
- Increase Redis maxmemory
- Set appropriate eviction policy
- Clear unused keys

### "READONLY You can't write against a read only replica"
- Check Redis role (master/slave)
- Verify connection to master

### "WRONGTYPE Operation against a key holding the wrong kind of value"
- Key type mismatch (string vs hash vs set)
- Delete and recreate with correct type

## Best Practices

✅ **DO:**
- Always set TTL on keys
- Use namespaced key prefixes
- Handle Redis failures gracefully
- Monitor memory usage
- Use connection pooling
- Set Redis password in production

❌ **DON'T:**
- Store large values (> 100KB)
- Use KEYS in production (use SCAN)
- Forget TTL on temporary data
- Store sensitive data without encryption
- Use blocking operations
- Run FLUSHDB in production

## Resources

- Full Documentation: `docs/REDIS_INTEGRATION.md`
- Implementation Summary: `docs/REDIS_IMPLEMENTATION_SUMMARY.md`
- Spring Data Redis: https://spring.io/projects/spring-data-redis
- Redis Documentation: https://redis.io/documentation
