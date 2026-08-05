# Refresh Token Implementation

## Overview

This document describes the secure Refresh Token implementation for the Ziboto authentication system.

## Features

### 1. **Secure Token Storage**
- Refresh tokens are **hashed with BCrypt** (60-character hash) before storage in PostgreSQL
- Plain tokens are **never stored** in the database
- Each token hash is **unique** and indexed for fast lookup

### 2. **Redis Session Management**
- Active sessions tracked in Redis with user metadata
- Session cache with configurable TTL (1 hour default)
- Multi-device support with device-specific session tracking
- Fast session invalidation on logout

### 3. **Token Rotation**
- **Automatic token rotation** on refresh
- Old refresh token invalidated immediately after use
- New refresh token generated and returned
- Prevents token reuse attacks

### 4. **Multi-Device Support**
- Each device gets a separate refresh token
- Device information tracked (deviceInfo, userAgent, ipAddress)
- Users can see and manage active sessions
- "Logout all devices" functionality supported

### 5. **Security Features**
- Rate limiting on refresh attempts
- Blacklist checking for revoked tokens
- Token expiration (7 days default)
- Audit logging for all token operations
- Failed refresh attempt tracking

## API Endpoints

### Refresh Token Endpoint

**POST** `/api/v1/auth/refresh`

Obtain a new access token using a valid refresh token.

#### Request Body

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "username": "john.doe",
      "email": "john.doe@example.com",
      "role": "USER",
      "status": "ACTIVE",
      "createdAt": "2024-01-15T10:30:00",
      "lastLoginAt": "2024-01-20T14:45:00"
    }
  }
}
```

#### Error Responses

**401 Unauthorized** - Invalid or expired refresh token
```json
{
  "success": false,
  "message": "Refresh token is invalid or expired",
  "timestamp": "2024-01-20T15:30:00"
}
```

**429 Too Many Requests** - Rate limit exceeded
```json
{
  "success": false,
  "message": "Too many token refresh attempts. Please try again later.",
  "timestamp": "2024-01-20T15:30:00"
}
```

## Refresh Token Flow

### Complete Flow Diagram

```
┌─────────────┐                                    ┌──────────────┐
│   Client    │                                    │   Backend    │
└──────┬──────┘                                    └──────┬───────┘
       │                                                  │
       │ 1. POST /api/v1/auth/refresh                    │
       │    { refreshToken: "..." }                      │
       ├────────────────────────────────────────────────>│
       │                                                  │
       │                         2. Validate JWT format  │
       │                         3. Check blacklist      │
       │                         4. Extract username     │
       │                         5. Check rate limit     │
       │                         6. Query PostgreSQL     │
       │                            (fetch active tokens)│
       │                         7. BCrypt validation    │
       │                            (compare hashes)     │
       │                         8. Verify not revoked   │
       │                         9. Verify not expired   │
       │                        10. Generate new access  │
       │                        11. Generate new refresh │
       │                        12. Hash new refresh     │
       │                        13. Store in PostgreSQL  │
       │                        14. Revoke old token     │
       │                        15. Update Redis session │
       │                        16. Create audit log     │
       │                                                  │
       │ 17. Return new tokens                           │
       │<────────────────────────────────────────────────┤
       │                                                  │
```

### Step-by-Step Process

1. **Client Request**
   - Client sends POST request with refresh token in body
   - IP address extracted from request headers

2. **JWT Validation**
   - Validate JWT format and signature
   - Check if token is well-formed
   - Extract claims (username, expiration)

3. **Blacklist Check**
   - Query Redis for blacklisted tokens
   - Reject if token found in blacklist

4. **User Lookup**
   - Extract username from token
   - Retrieve user from PostgreSQL
   - Validate user status (ACTIVE)

5. **Rate Limiting**
   - Check Redis for refresh rate limit
   - Allow max N refreshes per time window
   - Record attempt

6. **Token Validation**
   - Query PostgreSQL for active refresh tokens
   - Filter by user, non-revoked, non-expired
   - BCrypt compare plain token against each hash
   - Return matching token record

7. **Token Generation**
   - Generate new access token (15 min TTL)
   - Generate new refresh token (7 day TTL)
   - Hash new refresh token with BCrypt

8. **Token Rotation**
   - Mark old refresh token as revoked
   - Save new hashed refresh token
   - Update device and IP information

9. **Redis Session Update**
   - Remove old session tracking
   - Add new session tracking
   - Update session cache with user data

10. **Audit Logging**
    - Create audit log entry
    - Record device, IP, timestamp
    - Log token rotation event

11. **Response**
    - Return new access token
    - Return new refresh token
    - Include user information

## Database Schema

### refresh_tokens Table

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(60) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    last_used_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_expires_at ON refresh_tokens(expires_at);
```

### Field Descriptions

- **id**: Unique identifier (UUID)
- **token_hash**: BCrypt hash of refresh token (60 chars)
- **user_id**: Foreign key to users table
- **expires_at**: Token expiration timestamp (7 days)
- **revoked**: Manual revocation flag
- **created_at**: Token creation timestamp
- **device_info**: Device type (Mobile, Desktop, etc.)
- **ip_address**: IP address of token creation
- **user_agent**: Full user agent string
- **last_used_at**: Last token usage timestamp

## Redis Keys

### Session Cache
- **Key**: `session:user:{username}`
- **Value**: UserResponse object
- **TTL**: 1 hour (refreshed on access)

### Active Sessions
- **Key**: `session:active:{username}`
- **Value**: Hash of sessionId → deviceInfo
- **TTL**: 24 hours

### Token Blacklist
- **Key**: `token:blacklist:{token}`
- **Value**: username
- **TTL**: Remaining token lifetime

## Security Considerations

### 1. **BCrypt Hashing**
- Strength: 10 rounds (default)
- 60-character hash output
- Computationally expensive to crack
- Salt automatically generated per token

### 2. **Token Rotation**
- Old token invalidated immediately
- Prevents token reuse
- Limits impact of token compromise
- Forces re-authentication if token stolen

### 3. **Rate Limiting**
- Prevents brute force attacks
- Configurable limits per user
- Exponential backoff on failures

### 4. **Expiration**
- Access tokens: 15 minutes
- Refresh tokens: 7 days
- Automatic cleanup of expired tokens

### 5. **Audit Trail**
- All token operations logged
- Device and IP tracking
- Suspicious activity detection

## Multi-Device Support

### Device Tracking

Each device gets:
- Separate refresh token
- Unique session ID
- Device information record
- Independent expiration

### Active Sessions View

Users can:
- See all active devices
- View last used timestamp
- Revoke individual sessions
- Logout all devices at once

### Implementation

```java
// Track new session
sessionCacheService.trackActiveSession(
    username,
    refreshToken.getId().toString(),
    ipAddress
);

// Get all active sessions
Map<String, Object> sessions = sessionCacheService.getActiveSessions(username);

// Revoke specific session
refreshTokenService.revokeToken(tokenId);
sessionCacheService.removeActiveSession(username, tokenId);

// Logout all devices
refreshTokenService.revokeAllUserTokens(userId);
sessionCacheService.clearAllActiveSessions(username);
```

## Configuration

### Application Properties

```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 900000  # 15 minutes (ms)
      refresh-expiration: 604800000  # 7 days (ms)
```

### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
```

## Usage Examples

### Frontend Integration

```typescript
// Refresh token function
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  
  try {
    const response = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });
    
    if (response.ok) {
      const data = await response.json();
      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
      return data.data.accessToken;
    } else {
      // Refresh failed, redirect to login
      window.location.href = '/login';
    }
  } catch (error) {
    console.error('Token refresh failed:', error);
    window.location.href = '/login';
  }
}

// Axios interceptor for automatic refresh
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      const newAccessToken = await refreshAccessToken();
      
      if (newAccessToken) {
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
        return axios(originalRequest);
      }
    }
    
    return Promise.reject(error);
  }
);
```

## Testing

### Manual Testing with cURL

```bash
# 1. Login to get tokens
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john.doe",
    "password": "Password123"
  }'

# Response includes refreshToken

# 2. Refresh the token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'

# 3. Try to reuse old refresh token (should fail)
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "OLD_TOKEN_HERE"
  }'
```

## Maintenance

### Cleanup Tasks

Run periodic cleanup to remove expired and revoked tokens:

```java
// Schedule with @Scheduled annotation
@Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
public void cleanupTokens() {
    // Remove expired tokens
    refreshTokenService.cleanupExpiredTokens();
    
    // Remove old revoked tokens (older than 30 days)
    refreshTokenService.cleanupOldRevokedTokens(30);
}
```

## Monitoring

### Metrics to Track

- Refresh token success rate
- Failed refresh attempts per user
- Average tokens per user
- Token expiration patterns
- Device distribution
- Suspicious activity (rapid refreshes, unusual IPs)

### Audit Queries

```sql
-- Find users with multiple devices
SELECT user_id, COUNT(*) as device_count
FROM refresh_tokens
WHERE revoked = false AND expires_at > NOW()
GROUP BY user_id
HAVING COUNT(*) > 3;

-- Find inactive tokens
SELECT * FROM refresh_tokens
WHERE revoked = false 
  AND expires_at > NOW()
  AND last_used_at < NOW() - INTERVAL '7 days';

-- Find tokens from suspicious IPs
SELECT DISTINCT ip_address, COUNT(*) as token_count
FROM refresh_tokens
WHERE created_at > NOW() - INTERVAL '1 hour'
GROUP BY ip_address
HAVING COUNT(*) > 10;
```

## Troubleshooting

### Common Issues

1. **"Refresh token is invalid or expired"**
   - Token has expired (> 7 days old)
   - Token was revoked during logout
   - Token was used and rotated (old token)
   - Solution: Login again

2. **"Too many token refresh attempts"**
   - Rate limit exceeded
   - Wait for rate limit window to reset
   - Check for infinite refresh loops in frontend

3. **"User account is not active"**
   - User account suspended or deleted
   - Contact support to reactivate

4. **Token not found in database**
   - Token was cleaned up (expired)
   - Database connection issue
   - Check logs for errors

## Best Practices

1. **Store tokens securely**
   - Use httpOnly cookies for web apps
   - Use secure storage on mobile (Keychain/Keystore)
   - Never log tokens

2. **Implement automatic refresh**
   - Refresh before access token expires
   - Use interceptors for seamless UX
   - Handle refresh failures gracefully

3. **Logout properly**
   - Always revoke refresh tokens on logout
   - Clear all client-side token storage
   - Inform user of active sessions

4. **Monitor for abuse**
   - Track refresh patterns
   - Alert on suspicious activity
   - Implement device fingerprinting

5. **Regular cleanup**
   - Schedule expired token cleanup
   - Remove old revoked tokens
   - Monitor database growth

## Future Enhancements

1. **Enhanced device detection**
   - Parse user agent with library
   - Store device fingerprint
   - Detect new device logins

2. **Geographic tracking**
   - IP to location mapping
   - Alert on unusual locations
   - Location-based access policies

3. **Token families**
   - Link related tokens
   - Detect token theft chains
   - Revoke entire token family

4. **Configurable expiration**
   - Per-user token lifetime
   - Role-based expiration
   - "Remember me" functionality

5. **Advanced analytics**
   - Token usage dashboards
   - Security event correlation
   - Anomaly detection
