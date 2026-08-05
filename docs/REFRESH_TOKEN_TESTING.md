# Refresh Token Testing Guide

## Prerequisites

1. PostgreSQL database running
2. Redis server running
3. Backend application started
4. Valid user account created

## Test Scenarios

### Test 1: Successful Token Refresh

**Objective**: Verify that a valid refresh token can be used to obtain new tokens.

**Steps**:

1. Login to get initial tokens
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "Password123"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { ... }
  }
}
```

2. Use the refresh token to get new tokens
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "PASTE_REFRESH_TOKEN_HERE"
  }'
```

**Expected Response**: Same structure as login, but with new tokens

**Verification**:
- ✅ Status code is 200
- ✅ New access token is different from original
- ✅ New refresh token is different from original
- ✅ User information is included
- ✅ Old refresh token is now invalid

---

### Test 2: Token Rotation (Prevent Reuse)

**Objective**: Verify that old refresh tokens cannot be reused after refresh.

**Steps**:

1. Get initial tokens (login)
2. Save the refresh token: `OLD_REFRESH_TOKEN`
3. Refresh the token once (successful)
4. Try to use `OLD_REFRESH_TOKEN` again

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "OLD_REFRESH_TOKEN"
  }'
```

**Expected Response**: 401 Unauthorized
```json
{
  "success": false,
  "message": "Refresh token is invalid or expired",
  "timestamp": "2024-01-20T15:30:00"
}
```

**Verification**:
- ✅ Status code is 401
- ✅ Error message indicates invalid token
- ✅ Old token marked as revoked in database

**Database Check**:
```sql
SELECT id, revoked, created_at, last_used_at 
FROM refresh_tokens 
WHERE user_id = (SELECT id FROM users WHERE username = 'testuser')
ORDER BY created_at DESC;
```

---

### Test 3: Expired Refresh Token

**Objective**: Verify that expired refresh tokens are rejected.

**Steps**:

1. Modify the JWT secret or manually create an expired token
2. Attempt to refresh with expired token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "EXPIRED_TOKEN"
  }'
```

**Expected Response**: 401 Unauthorized
```json
{
  "success": false,
  "message": "Refresh token is invalid or expired"
}
```

**Verification**:
- ✅ Status code is 401
- ✅ No new tokens generated
- ✅ No database records created

---

### Test 4: Invalid Refresh Token Format

**Objective**: Verify that malformed tokens are rejected.

**Steps**:

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "invalid-token-format"
  }'
```

**Expected Response**: 401 Unauthorized

**Verification**:
- ✅ Status code is 401
- ✅ Validation happens before database query

---

### Test 5: Rate Limiting on Refresh

**Objective**: Verify rate limiting prevents abuse.

**Steps**:

1. Get valid refresh token
2. Make rapid refresh requests (10+ within 1 minute)

```bash
# Run in a loop
for i in {1..15}; do
  curl -X POST http://localhost:8080/api/v1/auth/refresh \
    -H "Content-Type: application/json" \
    -d '{"refreshToken": "VALID_TOKEN"}' &
done
wait
```

**Expected Response** (after threshold): 429 Too Many Requests
```json
{
  "success": false,
  "message": "Too many token refresh attempts. Please try again later."
}
```

**Verification**:
- ✅ First N requests succeed
- ✅ Subsequent requests return 429
- ✅ Rate limit enforced via Redis

**Redis Check**:
```bash
redis-cli GET "ratelimit:refresh:USER_ID"
```

---

### Test 6: Multi-Device Support

**Objective**: Verify multiple devices can have active refresh tokens simultaneously.

**Steps**:

1. Login from "Device 1" (simulate with different IP or user agent)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0)" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "Password123"
  }'
```
Save `DEVICE_1_REFRESH_TOKEN`

2. Login from "Device 2"
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "User-Agent: Mozilla/5.0 (iPhone)" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "Password123"
  }'
```
Save `DEVICE_2_REFRESH_TOKEN`

3. Refresh both tokens independently
```bash
# Refresh Device 1
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "DEVICE_1_REFRESH_TOKEN"}'

# Refresh Device 2
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "DEVICE_2_REFRESH_TOKEN"}'
```

**Expected Response**: Both succeed independently

**Verification**:
- ✅ Both devices have active tokens
- ✅ Refreshing one doesn't affect the other
- ✅ Database shows 2 active tokens

**Database Check**:
```sql
SELECT id, device_info, ip_address, created_at, revoked
FROM refresh_tokens
WHERE user_id = (SELECT id FROM users WHERE username = 'testuser')
  AND revoked = false
ORDER BY created_at DESC;
```

Expected: 2 rows (one per device)

---

### Test 7: Token Blacklisting After Logout

**Objective**: Verify refresh tokens are invalidated after logout.

**Steps**:

1. Login and get tokens
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "Password123"
  }'
```

2. Logout
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

3. Try to refresh with the old token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "REFRESH_TOKEN_FROM_STEP_1"}'
```

**Expected Response**: 401 Unauthorized

**Verification**:
- ✅ Refresh fails after logout
- ✅ Token marked as revoked in database
- ✅ Redis session cleared

**Database Check**:
```sql
SELECT revoked FROM refresh_tokens 
WHERE user_id = (SELECT id FROM users WHERE username = 'testuser');
```

Expected: All tokens have `revoked = true`

---

### Test 8: Security - BCrypt Hash Storage

**Objective**: Verify tokens are hashed in the database.

**Steps**:

1. Login and note the refresh token
```bash
REFRESH_TOKEN="eyJhbGc..."
```

2. Query database to check stored value
```sql
SELECT token_hash FROM refresh_tokens 
ORDER BY created_at DESC LIMIT 1;
```

**Verification**:
- ✅ `token_hash` is 60 characters long
- ✅ `token_hash` starts with `$2a$` or `$2b$` (BCrypt format)
- ✅ `token_hash` ≠ plain refresh token
- ✅ No plain token stored in database

**Example Hash**:
```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92lJP8YE.5o8VBGzXF1fK
```

---

### Test 9: Refresh with Suspended User

**Objective**: Verify suspended users cannot refresh tokens.

**Steps**:

1. Login as normal user
2. Suspend the user account
```sql
UPDATE users SET status = 'SUSPENDED' 
WHERE username = 'testuser';
```

3. Try to refresh token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "VALID_TOKEN"}'
```

**Expected Response**: 403 Forbidden or 401 Unauthorized
```json
{
  "success": false,
  "message": "Account has been suspended. Please contact support."
}
```

**Verification**:
- ✅ Refresh fails for suspended users
- ✅ Appropriate error message returned

---

### Test 10: Session Tracking in Redis

**Objective**: Verify active sessions are tracked in Redis.

**Steps**:

1. Login and note the refresh token ID
2. Check Redis for session tracking

```bash
# Check active sessions
redis-cli HGETALL "session:active:testuser"

# Check session cache
redis-cli GET "session:user:testuser"
```

**Expected Output**:
```
# Active sessions
1) "TOKEN_UUID_1"
2) "Desktop"
3) "TOKEN_UUID_2"
4) "Mobile Device"

# Session cache (JSON)
{"id":1,"username":"testuser","email":"...","role":"USER"}
```

**Verification**:
- ✅ Active sessions tracked in Redis
- ✅ Device information stored
- ✅ User session cached
- ✅ TTL set appropriately

---

### Test 11: Concurrent Refresh Attempts

**Objective**: Verify system handles concurrent refreshes gracefully.

**Steps**:

1. Get a valid refresh token
2. Make 5 simultaneous refresh requests

```bash
TOKEN="VALID_REFRESH_TOKEN"

curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$TOKEN\"}" &

curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$TOKEN\"}" &

curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$TOKEN\"}" &

wait
```

**Expected Behavior**:
- First request succeeds
- Subsequent requests fail (token already rotated)

**Verification**:
- ✅ Only one refresh succeeds
- ✅ No duplicate tokens created
- ✅ Database consistency maintained

---

### Test 12: Audit Log Creation

**Objective**: Verify all refresh operations are audited.

**Steps**:

1. Perform a successful token refresh
2. Query audit logs

```sql
SELECT * FROM audit_logs 
WHERE action = 'TOKEN_REFRESH' 
  AND user_id = (SELECT id FROM users WHERE username = 'testuser')
ORDER BY created_at DESC 
LIMIT 5;
```

**Expected Output**:
```
id | user_id | entity_type | entity_id | action        | details                              | created_at
---|---------|-------------|-----------|---------------|--------------------------------------|-------------------
1  | 1       | User        | 1         | TOKEN_REFRESH | Token refreshed from IP: 127.0.0.1... | 2024-01-20 15:30:00
```

**Verification**:
- ✅ Audit log entry created
- ✅ IP address recorded
- ✅ Device information included
- ✅ Timestamp accurate

---

## Database Verification Queries

### Check All Active Tokens for User
```sql
SELECT 
    id,
    user_id,
    LEFT(token_hash, 20) as token_hash_preview,
    device_info,
    ip_address,
    created_at,
    expires_at,
    last_used_at,
    revoked
FROM refresh_tokens
WHERE user_id = (SELECT id FROM users WHERE username = 'testuser')
  AND revoked = false
  AND expires_at > NOW()
ORDER BY created_at DESC;
```

### Check Token Rotation History
```sql
SELECT 
    id,
    device_info,
    created_at,
    last_used_at,
    revoked,
    expires_at
FROM refresh_tokens
WHERE user_id = (SELECT id FROM users WHERE username = 'testuser')
ORDER BY created_at DESC
LIMIT 10;
```

### Find Suspicious Activity
```sql
-- Multiple tokens from same IP in short time
SELECT 
    ip_address,
    COUNT(*) as token_count,
    MIN(created_at) as first_seen,
    MAX(created_at) as last_seen
FROM refresh_tokens
WHERE created_at > NOW() - INTERVAL '1 hour'
GROUP BY ip_address
HAVING COUNT(*) > 5
ORDER BY token_count DESC;
```

## Redis Verification Commands

### Check Rate Limiting
```bash
# Check refresh rate limit for user
redis-cli GET "ratelimit:refresh:USER_ID"

# Check login rate limit
redis-cli GET "ratelimit:login:username"
```

### Check Session Data
```bash
# Get cached user session
redis-cli GET "session:user:testuser"

# Get active sessions
redis-cli HGETALL "session:active:testuser"

# Check token blacklist
redis-cli GET "token:blacklist:TOKEN_STRING"
```

### Monitor Redis in Real-Time
```bash
# Monitor all Redis operations
redis-cli MONITOR

# Check all keys with pattern
redis-cli KEYS "session:*"
redis-cli KEYS "token:*"
```

## Performance Testing

### Load Test: Refresh Endpoint

Using Apache Bench (ab):

```bash
# 1000 requests, 10 concurrent
ab -n 1000 -c 10 \
   -H "Content-Type: application/json" \
   -p refresh_body.json \
   http://localhost:8080/api/v1/auth/refresh
```

**refresh_body.json**:
```json
{"refreshToken": "VALID_TOKEN"}
```

**Expected Metrics**:
- Requests per second: > 100
- Average response time: < 100ms
- Failed requests: < 1%

---

## Automated Test Suite (Optional)

Create integration tests using JUnit:

```java
@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testSuccessfulRefresh() throws Exception {
        // Login
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"usernameOrEmail\":\"test\",\"password\":\"pass\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        
        String refreshToken = extractRefreshToken(loginResponse);
        
        // Refresh
        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists());
    }
    
    @Test
    void testTokenReusePrevention() throws Exception {
        // Get tokens and refresh once
        String oldToken = getAndRefreshToken();
        
        // Try to reuse old token
        mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + oldToken + "\"}"))
            .andExpect(status().isUnauthorized());
    }
}
```

## Troubleshooting

### Issue: "No matching hash found"
**Cause**: Token validation failed
**Solution**: 
- Verify BCrypt is working correctly
- Check if token was modified
- Ensure database contains the token hash

### Issue: Rate limit triggered unexpectedly
**Cause**: Redis rate limit counter not reset
**Solution**:
```bash
redis-cli DEL "ratelimit:refresh:USER_ID"
```

### Issue: Session not found in Redis
**Cause**: Redis cleared or TTL expired
**Solution**: Session will be recreated on next operation

## Success Criteria

All tests pass when:

- ✅ Valid refresh tokens generate new token pairs
- ✅ Old tokens cannot be reused (rotation works)
- ✅ Expired tokens are rejected
- ✅ Invalid tokens are rejected
- ✅ Rate limiting prevents abuse
- ✅ Multiple devices supported simultaneously
- ✅ Logout revokes all tokens
- ✅ Tokens stored as BCrypt hashes
- ✅ Suspended users cannot refresh
- ✅ Redis tracks active sessions
- ✅ Concurrent requests handled safely
- ✅ Audit logs created for all operations

## Continuous Monitoring

Set up alerts for:
- Unusual refresh patterns (> 100/hour per user)
- High rate limit rejections
- Failed refresh attempts spike
- Token reuse attempts
- Tokens from suspicious IPs
