# Refresh Token - Quick Reference Card

## 🚀 Quick Start

### API Endpoint
```
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Response
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "NEW_ACCESS_TOKEN",
    "refreshToken": "NEW_REFRESH_TOKEN",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { "id": 1, "username": "john", ... }
  }
}
```

## 📊 Token Lifecycles

| Token Type | Lifetime | Storage | Rotation |
|------------|----------|---------|----------|
| Access Token | 15 minutes | Client memory | No |
| Refresh Token | 7 days | Client storage | Yes (on use) |
| Token Hash | 7 days | PostgreSQL | - |
| Session Cache | 1 hour | Redis | Auto-refresh |

## 🔐 Security Features

- ✅ BCrypt hashing (strength 10)
- ✅ Automatic token rotation
- ✅ Token reuse prevention
- ✅ Rate limiting
- ✅ Multi-device support
- ✅ Blacklist checking
- ✅ Audit logging

## 📁 Key Files

### Backend
- `RefreshToken.java` - Entity (tokenHash field)
- `RefreshTokenService.java` - Token management
- `RefreshTokenRepository.java` - Database queries
- `AuthServiceImpl.java` - Refresh logic
- `V6__Create_refresh_tokens_table.sql` - Migration

### Database
```sql
-- Table
refresh_tokens (id, token_hash, user_id, expires_at, ...)

-- Indexes
idx_refresh_token_hash, idx_user_id, idx_expires_at
```

### Redis Keys
```
session:user:{username}          → User cache
session:active:{username}        → Active sessions
token:blacklist:{token}          → Blacklisted tokens
```

## 🔄 Complete Flow

```
1. Client sends refresh token
   ↓
2. Validate JWT format
   ↓
3. Check Redis blacklist
   ↓
4. Query PostgreSQL for active tokens
   ↓
5. BCrypt validate against hashes
   ↓
6. Verify not revoked/expired
   ↓
7. Generate new access token
   ↓
8. Generate new refresh token
   ↓
9. Hash and store new token
   ↓
10. Revoke old token
    ↓
11. Update Redis session
    ↓
12. Return new tokens
```

## 🧪 Quick Test

```bash
# 1. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"user","password":"pass"}'

# Save refreshToken from response

# 2. Refresh
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"TOKEN_HERE"}'

# 3. Try old token (should fail)
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"OLD_TOKEN"}'
```

## 📝 Database Queries

### View Active Tokens
```sql
SELECT id, device_info, ip_address, created_at, revoked
FROM refresh_tokens
WHERE user_id = 1 AND revoked = false;
```

### Count User Devices
```sql
SELECT user_id, COUNT(*) as devices
FROM refresh_tokens
WHERE revoked = false AND expires_at > NOW()
GROUP BY user_id;
```

### Recent Activity
```sql
SELECT * FROM audit_logs
WHERE action = 'TOKEN_REFRESH'
ORDER BY created_at DESC
LIMIT 10;
```

## 🔧 Redis Commands

```bash
# Check user session
redis-cli GET "session:user:username"

# View active sessions
redis-cli HGETALL "session:active:username"

# Check blacklist
redis-cli GET "token:blacklist:TOKEN"

# Clear user session
redis-cli DEL "session:user:username"
```

## ⚙️ Configuration

```yaml
# application.yml
app:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 900000        # 15 min
      refresh-expiration: 604800000  # 7 days
```

## 🐛 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| 401 Unauthorized | Token expired/invalid | Login again |
| 429 Too Many Requests | Rate limit hit | Wait and retry |
| Token reuse fails | Already rotated | Use new token |
| Hash mismatch | Wrong token | Check token value |

## 📊 Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Use new tokens |
| 401 | Unauthorized | Redirect to login |
| 429 | Rate limited | Show retry message |
| 500 | Server error | Retry or contact support |

## 🎯 Frontend Integration

```typescript
// Axios interceptor
apiClient.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      const newToken = await refreshAccessToken();
      error.config.headers.Authorization = `Bearer ${newToken}`;
      return apiClient(error.config);
    }
    return Promise.reject(error);
  }
);
```

## 📈 Monitoring

### Key Metrics
- Refresh success rate
- Failed refresh attempts
- Tokens per user
- Device distribution
- Suspicious activity

### Alerts
- High failure rate (> 10%)
- Unusual refresh patterns
- Token reuse attempts
- Rate limit exceeded

## 🔒 Security Checklist

- [x] Tokens hashed with BCrypt
- [x] Token rotation on refresh
- [x] Old tokens immediately revoked
- [x] Rate limiting enabled
- [x] Blacklist checking active
- [x] Multi-device support
- [x] Audit logging enabled
- [x] HTTPS in production

## 📚 Documentation

- **Full Implementation**: `REFRESH_TOKEN_IMPLEMENTATION.md`
- **Testing Guide**: `REFRESH_TOKEN_TESTING.md`
- **Frontend Guide**: `FRONTEND_INTEGRATION_GUIDE.md`
- **Summary**: `REFRESH_TOKEN_SUMMARY.md`

## 🆘 Support Commands

```bash
# Check logs
tail -f logs/ziboto.log | grep TOKEN_REFRESH

# Verify database
psql -U ziboto -d ziboto -c "SELECT COUNT(*) FROM refresh_tokens;"

# Test Redis
redis-cli PING

# Check service health
curl http://localhost:8080/actuator/health
```

## 💡 Pro Tips

1. **Refresh proactively** - Don't wait for 401, refresh before expiration
2. **Handle race conditions** - Queue concurrent requests during refresh
3. **Log strategically** - Log refresh events but never log token values
4. **Monitor patterns** - Alert on unusual refresh frequencies
5. **Clean regularly** - Schedule cleanup of expired tokens
6. **Test thoroughly** - Verify rotation, rate limiting, and multi-device

## ✅ Production Checklist

- [ ] JWT_SECRET set (production value)
- [ ] Redis connection configured
- [ ] PostgreSQL migration V6 applied
- [ ] Rate limits tuned
- [ ] Monitoring configured
- [ ] Logs aggregated
- [ ] Frontend integrated
- [ ] End-to-end tested

---

**Version**: 1.0  
**Last Updated**: 2024  
**Status**: Production Ready ✅
