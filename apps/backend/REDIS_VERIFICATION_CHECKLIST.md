# Redis Implementation Verification Checklist

Use this checklist to verify the Redis integration is working correctly.

## ☑️ Prerequisites

- [ ] Redis 7+ installed and running
- [ ] Java 21+ installed
- [ ] Maven 3.9+ installed
- [ ] Environment variables configured

## ☑️ Quick Verification Steps

### 1. Start Redis
```bash
# Using Docker
docker run -d --name ziboto-redis -p 6379:6379 redis:7-alpine

# Verify Redis is running
redis-cli ping
# Expected: PONG
```
**Status:** ___________

### 2. Check Environment Configuration
```bash
# Verify .env file has Redis configuration
cat apps/backend/.env | grep REDIS_HOST
```
**Status:** ___________

### 3. Build Application
```bash
cd apps/backend
./mvnw clean compile -DskipTests
```
**Expected:** BUILD SUCCESS  
**Status:** ___________

### 4. Start Application
```bash
./mvnw spring-boot:run
```
**Expected:** Application starts without Redis errors  
**Status:** ___________

## ☑️ Feature Verification

### Rate Limiting

#### Test Login Rate Limiting
```bash
# Make 6 login attempts (should fail on 6th)
for i in {1..6}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}'
  echo "Attempt $i"
done
```
**Expected:** 6th attempt returns 429 Too Many Requests  
**Status:** ___________

#### Verify in Redis
```bash
redis-cli KEYS "rate_limit:login:*"
redis-cli GET "rate_limit:login:test"
redis-cli TTL "rate_limit:login:test"
```
**Expected:** Key exists with value 6 and TTL ~900 seconds  
**Status:** ___________

### Failed Login Attempts

#### Test Account Lockout
```bash
# Make 5 failed login attempts
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"locktest","password":"wrong"}'
  echo "Attempt $i"
done

# 6th attempt should be locked
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"locktest","password":"wrong"}'
```
**Expected:** Account locked message after 5 attempts  
**Status:** ___________

#### Verify in Redis
```bash
redis-cli KEYS "failed_login:*:locktest"
redis-cli GET "failed_login:attempts:locktest"
redis-cli EXISTS "failed_login:lockout:locktest"
```
**Expected:** Attempt count = 5, lockout key exists  
**Status:** ___________

### Session Caching

#### Test Session Cache
```bash
# Login successfully
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.accessToken')

# Make authenticated request
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```
**Expected:** User data returned, session cached in Redis  
**Status:** ___________

#### Verify in Redis
```bash
redis-cli KEYS "session:user:*"
redis-cli GET "session:user:admin"
redis-cli TTL "session:user:admin"
```
**Expected:** Session key exists with TTL ~3600 seconds  
**Status:** ___________

### Token Blacklisting

#### Test Token Blacklist
```bash
# Logout (blacklists token)
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"

# Try to use blacklisted token
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```
**Expected:** 401 Unauthorized after logout  
**Status:** ___________

#### Verify in Redis
```bash
redis-cli KEYS "token:blacklist:*"
redis-cli EXISTS "token:blacklist:token:$TOKEN"
```
**Expected:** Token exists in blacklist  
**Status:** ___________

### OTP Generation

#### Test OTP Service (Code)
```java
@Autowired
private OtpCacheService otpCacheService;

@Test
void testOtpGeneration() {
    String otp = otpCacheService.generateOtp(
        "test@example.com", 
        OtpPurpose.EMAIL_VERIFICATION
    );
    
    assertNotNull(otp);
    assertEquals(6, otp.length());
    
    // Verify
    boolean valid = otpCacheService.verifyOtp(
        "test@example.com",
        otp,
        OtpPurpose.EMAIL_VERIFICATION
    );
    
    assertTrue(valid);
}
```
**Status:** ___________

#### Verify in Redis
```bash
redis-cli KEYS "otp:*"
redis-cli GET "otp:otp:email_verification:test@example.com"
redis-cli TTL "otp:otp:email_verification:test@example.com"
```
**Expected:** OTP exists with TTL ~300 seconds  
**Status:** ___________

## ☑️ Configuration Verification

### Check All Services Are Using RedisService
```bash
# Verify services inject RedisService
grep -r "private final RedisService" apps/backend/src/main/java/com/ziboto/backend/auth/service/
```
**Expected:** All services (Rate, Failed, Session, Token, OTP) inject RedisService  
**Status:** ___________

### Check All Services Use RedisProperties
```bash
# Verify services inject RedisProperties
grep -r "private final RedisProperties" apps/backend/src/main/java/com/ziboto/backend/auth/service/
```
**Expected:** All services inject RedisProperties  
**Status:** ___________

### Verify Configuration Loading
```bash
# Start application and check logs for configuration
./mvnw spring-boot:run | grep -i redis
```
**Expected:** No configuration errors, Redis connected  
**Status:** ___________

## ☑️ Error Handling Verification

### Test Redis Unavailable
```bash
# Stop Redis
docker stop ziboto-redis

# Try to login (should still work with degraded functionality)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Start Redis again
docker start ziboto-redis
```
**Expected:** Application continues to work (fail-open)  
**Status:** ___________

### Check Logs for Redis Errors
```bash
# Check logs
tail -f apps/backend/logs/ziboto.log | grep -i redis
```
**Expected:** Errors logged but application doesn't crash  
**Status:** ___________

## ☑️ Performance Verification

### Test Redis Response Time
```bash
redis-cli --latency
# Press Ctrl+C after a few seconds
```
**Expected:** Latency < 2ms  
**Status:** ___________

### Test Connection Pool
```bash
# Monitor Redis connections
redis-cli CLIENT LIST
```
**Expected:** ~2-8 connections (based on pool config)  
**Status:** ___________

### Check Memory Usage
```bash
redis-cli INFO memory
```
**Expected:** Reasonable memory usage  
**Status:** ___________

## ☑️ Documentation Verification

### Check All Documentation Exists
```bash
ls -la apps/backend/docs/REDIS_*.md
```
**Expected Files:**
- [x] REDIS_INTEGRATION.md
- [x] REDIS_IMPLEMENTATION_SUMMARY.md
- [x] REDIS_QUICK_REFERENCE.md
- [x] REDIS_ARCHITECTURE.md

**Status:** ___________

### Verify Configuration Examples
```bash
# Check .env.example has all Redis variables
grep "^REDIS_" apps/backend/.env.example | wc -l
```
**Expected:** 20+ Redis configuration variables  
**Status:** ___________

## ☑️ Code Quality Verification

### Check for Compilation Errors
```bash
./mvnw clean compile -DskipTests
```
**Expected:** BUILD SUCCESS, no errors  
**Status:** ___________

### Check for Code Warnings (Optional)
```bash
./mvnw clean compile -DskipTests 2>&1 | grep WARNING
```
**Expected:** Only deprecation warnings (known issue)  
**Status:** ___________

### Run Unit Tests (If Available)
```bash
./mvnw test -Dtest="*Redis*"
```
**Status:** ___________

## ☑️ Clean Up

### Clear Test Data
```bash
# Clear all test keys from Redis
redis-cli FLUSHDB
```
**Status:** ___________

### Stop Test Redis
```bash
docker stop ziboto-redis
docker rm ziboto-redis
```
**Status:** ___________

## 📝 Summary

### Overall Status
- [ ] All prerequisites met
- [ ] Application builds successfully
- [ ] Rate limiting works
- [ ] Failed login tracking works
- [ ] Session caching works
- [ ] Token blacklisting works
- [ ] OTP generation works
- [ ] Configuration verified
- [ ] Error handling works
- [ ] Performance acceptable
- [ ] Documentation complete

### Issues Encountered
1. _____________________________________
2. _____________________________________
3. _____________________________________

### Notes
_________________________________________
_________________________________________
_________________________________________

### Final Verification
**Date:** ___________  
**Verified By:** ___________  
**Status:** [ ] PASS / [ ] FAIL  

---

## Quick Commands Reference

### Start Redis
```bash
docker run -d --name ziboto-redis -p 6379:6379 redis:7-alpine
```

### Check Redis Status
```bash
redis-cli ping
```

### Monitor Redis
```bash
redis-cli MONITOR
```

### View All Keys
```bash
redis-cli KEYS "*"
```

### Check Specific Key
```bash
redis-cli GET "key_name"
redis-cli TTL "key_name"
```

### Clear Database (Dev Only!)
```bash
redis-cli FLUSHDB
```

### Build Application
```bash
./mvnw clean compile -DskipTests
```

### Run Application
```bash
./mvnw spring-boot:run
```

---

**Note:** This checklist should be completed in a development environment before deploying to staging or production.
