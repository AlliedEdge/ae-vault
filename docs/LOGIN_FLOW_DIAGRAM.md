# Login Flow Diagram

## Visual Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         POST /api/v1/auth/login                         │
│                     { username, password }                              │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         1. AuthController                               │
│  • Extract client IP address (X-Forwarded-For/X-Real-IP/RemoteAddr)   │
│  • Validate request body                                               │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      2. AuthServiceImpl.login()                         │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   3. Redis Rate Limit Check                             │
│  • Key: rate_limit:login:{identifier}                                  │
│  • Limit: 5 attempts per 15 minutes                                    │
│  • Action: Throw RateLimitExceededException if exceeded                │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                  4. Redis Failed Login Check                            │
│  • Key: failed_login:lockout:{identifier}                              │
│  • Lockout: 5 failed attempts = 30 minutes                             │
│  • Action: Throw AccountLockedException if locked                      │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              5. Retrieve User from PostgreSQL                           │
│  • Query: SELECT * FROM users WHERE username = ? OR email = ?          │
│  • Validate: User exists                                               │
│  • Validate: User status is ACTIVE                                     │
│  • Action: Record failed attempt if not found                          │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              6. BCrypt Password Verification                            │
│  • Method: passwordEncoder.matches(plain, hashed)                      │
│  • Algorithm: BCrypt with automatic salt                               │
│  • Action: Record failed attempt if incorrect                          │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                  ┌───────────────┴───────────────┐
                  │   PASSWORD MATCHES            │
                  └───────────────┬───────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              7. Generate Access Token (JWT)                             │
│  • Expiration: 15 minutes (900,000 ms)                                 │
│  • Algorithm: HS512                                                     │
│  • Claims: username, roles, type=access                                │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              8. Generate Refresh Token (JWT)                            │
│  • Expiration: 7 days (604,800,000 ms)                                 │
│  • Algorithm: HS512                                                     │
│  • Claims: username, type=refresh                                      │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              9. Store Session in Redis                                  │
│  • Key: session:user:{username}                                        │
│  • Value: UserResponse (id, username, email, role, etc.)              │
│  • TTL: 1 hour (sliding window)                                        │
│  • Track Active Session: session:active:{username}                     │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│            10. Store Refresh Token in PostgreSQL                        │
│  • Table: refresh_tokens                                               │
│  • Fields: token, user_id, expires_at, ip_address, device_info        │
│  • Revoked: false                                                      │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              11. Update Last Login Timestamp                            │
│  • UPDATE users SET last_login_at = NOW() WHERE id = ?                 │
│  • Field: user.lastLoginAt = LocalDateTime.now()                       │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              12. Create Audit Log (Async)                               │
│  • Table: audit_logs                                                   │
│  • Fields: user_id, action=LOGIN, details, ip_address, user_agent     │
│  • Processing: Non-blocking (async)                                    │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              13. Return Tokens and User Data                            │
│  • accessToken: "eyJhbGci..."                                          │
│  • refreshToken: "eyJhbGci..."                                         │
│  • tokenType: "Bearer"                                                 │
│  • expiresIn: 900                                                      │
│  • user: { id, username, email, role, status }                        │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              BONUS: Reset Security Counters                             │
│  • Reset rate limit counter                                            │
│  • Reset failed login attempts                                         │
│  • Clear lockout status                                                │
└─────────────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagram

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Client    │────▶│   Backend    │────▶│    Redis     │
│  (Browser)   │     │   (Spring)   │     │   (Cache)    │
└──────────────┘     └───────┬──────┘     └──────────────┘
                             │
                             │
                             ▼
                     ┌──────────────┐
                     │  PostgreSQL  │
                     │  (Database)  │
                     └──────────────┘
```

## Components Interaction

```
┌─────────────────────────────────────────────────────────────────┐
│                      Authentication Layer                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐       │
│  │ AuthController│──▶│ AuthService  │──▶│ JWT Provider │       │
│  └──────────────┘   └───────┬──────┘   └──────────────┘       │
│                             │                                    │
│                             │                                    │
└─────────────────────────────┼────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │ Rate Limit   │ │ Failed Login │ │   Session    │
    │   Service    │ │   Service    │ │   Service    │
    └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
           │                │                │
           │                │                │
           └────────────────┼────────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │    Redis     │
                    │   Template   │
                    └──────────────┘
              
              ┌───────────────┐
              │               │
              ▼               ▼
    ┌──────────────┐ ┌──────────────┐
    │    User      │ │    Audit     │
    │  Repository  │ │   Service    │
    └──────┬───────┘ └──────┬───────┘
           │                │
           └────────────────┘
                   │
                   ▼
            ┌──────────────┐
            │  PostgreSQL  │
            │   Database   │
            └──────────────┘
```

## Security Layers

```
┌──────────────────────────────────────────────────────────────────┐
│                        Request (Client)                          │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│ Layer 1: Rate Limiting (Redis)                                   │
│  • Prevents brute force attacks                                  │
│  • 5 attempts per 15 minutes                                     │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│ Layer 2: Account Lockout (Redis)                                 │
│  • Locks account after failed attempts                           │
│  • 30 minute lockout duration                                    │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│ Layer 3: User Validation (PostgreSQL)                            │
│  • User exists                                                   │
│  • Account status is ACTIVE                                      │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│ Layer 4: Password Verification (BCrypt)                          │
│  • Secure hash comparison                                        │
│  • Timing-attack resistant                                       │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│ Layer 5: Token Generation (JWT)                                  │
│  • HS512 algorithm                                               │
│  • Short-lived access tokens                                     │
│  • Long-lived refresh tokens                                     │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│ Layer 6: Session Tracking (Redis + PostgreSQL)                   │
│  • Session caching for performance                               │
│  • Refresh token persistence                                     │
│  • Audit logging                                                 │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                     Authenticated Session                         │
└──────────────────────────────────────────────────────────────────┘
```

## Token Lifecycle

```
┌────────────────────────────────────────────────────────────────────┐
│                        Login Success                               │
└────────────────┬───────────────────────────────────────────────────┘
                 │
                 ├──▶ Access Token (15 min)
                 │    • Used for API requests
                 │    • Short-lived for security
                 │    • Stored in memory (client)
                 │    • Bearer token in Authorization header
                 │
                 └──▶ Refresh Token (7 days)
                      • Used to get new access tokens
                      • Stored in PostgreSQL
                      • HttpOnly cookie (recommended)
                      • Rotated on each use

                After 15 minutes
                       │
                       ▼
┌────────────────────────────────────────────────────────────────────┐
│              Access Token Expires                                  │
└────────────────┬───────────────────────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────────────┐
│         POST /api/v1/auth/refresh                                  │
│         { refreshToken: "..." }                                    │
└────────────────┬───────────────────────────────────────────────────┘
                 │
                 ├──▶ Validate refresh token
                 │    • Check if exists in database
                 │    • Check if not revoked
                 │    • Check if not expired
                 │
                 ▼
┌────────────────────────────────────────────────────────────────────┐
│              Generate New Token Pair                               │
└────────────────┬───────────────────────────────────────────────────┘
                 │
                 ├──▶ New Access Token (15 min)
                 └──▶ New Refresh Token (7 days)
                      • Revoke old refresh token
                      • Store new refresh token
```

## Error Flow

```
┌────────────────────────────────────────────────────────────────────┐
│                     Login Attempt                                  │
└────────────────┬───────────────────────────────────────────────────┘
                 │
                 ▼
          ┌─────────────┐
          │  Is Valid?  │
          └──┬───────┬──┘
             │       │
         YES │       │ NO
             │       │
             ▼       ▼
      ┌──────────┐ ┌───────────────────────────────────┐
      │ Success  │ │  What Failed?                     │
      │ (200 OK) │ └───┬───┬───┬───┬────┬──────────────┘
      └──────────┘     │   │   │   │    │
                       │   │   │   │    │
                       │   │   │   │    └──▶ Invalid Credentials
                       │   │   │   │         (401 Unauthorized)
                       │   │   │   │         • Record failed attempt
                       │   │   │   │         • Show remaining attempts
                       │   │   │   │
                       │   │   │   └──────▶ Account Locked
                       │   │   │            (423 Locked)
                       │   │   │            • 5 failed attempts
                       │   │   │            • 30 min lockout
                       │   │   │
                       │   │   └──────────▶ Rate Limited
                       │   │                (429 Too Many Requests)
                       │   │                • 5 attempts per 15 min
                       │   │                • Show reset time
                       │   │
                       │   └──────────────▶ Account Suspended
                       │                    (423 Locked)
                       │                    • Admin suspended
                       │                    • Contact support
                       │
                       └──────────────────▶ Server Error
                                            (500 Internal Server Error)
                                            • Log error
                                            • Generic message
```

## Redis Key Structure

```
Redis Database 0
│
├── rate_limit:login:{identifier}
│   Value: Integer (attempt count)
│   TTL: 15 minutes
│   Purpose: Track login attempts for rate limiting
│
├── failed_login:attempts:{identifier}
│   Value: Integer (failed attempt count)
│   TTL: 1 hour
│   Purpose: Track failed login attempts
│
├── failed_login:lockout:{identifier}
│   Value: String (timestamp)
│   TTL: 30 minutes
│   Purpose: Account lockout flag
│
├── failed_login:last:{identifier}
│   Value: String (ISO timestamp)
│   TTL: 1 hour
│   Purpose: Last failed attempt time
│
├── session:user:{username}
│   Value: JSON (UserResponse)
│   TTL: 1 hour (sliding)
│   Purpose: Cache user session data
│
├── session:active:{username}
│   Value: Hash (sessionId -> deviceInfo)
│   TTL: 24 hours
│   Purpose: Track active sessions
│
└── token:blacklist:{token}
    Value: String ("revoked")
    TTL: Token remaining lifetime
    Purpose: Blacklist revoked tokens
```

## PostgreSQL Schema

```
┌─────────────────────────────────────────────────────────────────┐
│                         users                                    │
├──────────────┬─────────────┬────────────┬──────────────────────┤
│ id           │ BIGSERIAL   │ PK         │                      │
│ username     │ VARCHAR(50) │ UNIQUE     │ Index                │
│ email        │ VARCHAR(100)│ UNIQUE     │ Index                │
│ password     │ VARCHAR(255)│            │ BCrypt hashed        │
│ role         │ VARCHAR(20) │            │                      │
│ status       │ VARCHAR(20) │            │ Index                │
│ last_login_at│ TIMESTAMP   │            │ Index (NEW!)         │
│ created_at   │ TIMESTAMP   │            │ Index                │
│ updated_at   │ TIMESTAMP   │            │                      │
└──────────────┴─────────────┴────────────┴──────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     refresh_tokens                               │
├──────────────┬─────────────┬────────────┬──────────────────────┤
│ id           │ BIGSERIAL   │ PK         │                      │
│ token        │ VARCHAR(512)│ UNIQUE     │ Index                │
│ user_id      │ BIGINT      │ FK(users)  │ Index                │
│ expires_at   │ TIMESTAMP   │            │ Index                │
│ revoked      │ BOOLEAN     │            │                      │
│ ip_address   │ VARCHAR(45) │            │                      │
│ device_info  │ VARCHAR(255)│            │                      │
│ created_at   │ TIMESTAMP   │            │                      │
└──────────────┴─────────────┴────────────┴──────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       audit_logs                                 │
├──────────────┬─────────────┬────────────┬──────────────────────┤
│ id           │ BIGSERIAL   │ PK         │                      │
│ user_id      │ BIGINT      │ FK(users)  │ Index                │
│ entity_type  │ VARCHAR(50) │            │ Index                │
│ entity_id    │ BIGINT      │            │                      │
│ action       │ VARCHAR(20) │            │ Index (LOGIN)        │
│ details      │ TEXT        │            │                      │
│ ip_address   │ VARCHAR(45) │            │                      │
│ user_agent   │ VARCHAR(255)│            │                      │
│ created_at   │ TIMESTAMP   │            │ Index                │
└──────────────┴─────────────┴────────────┴──────────────────────┘
```

## Performance Metrics

```
┌──────────────────────────────────────────────────────────────┐
│                    Expected Timings                          │
├──────────────────────────────────────────────────────────────┤
│ Total Login Time:           < 500ms                          │
│  ├─ Rate Limit Check:       ~  10ms  (Redis)                │
│  ├─ Failed Login Check:     ~  10ms  (Redis)                │
│  ├─ User Query:             ~  50ms  (PostgreSQL)           │
│  ├─ Password Verification:  ~ 100ms  (BCrypt)               │
│  ├─ Token Generation:       ~  10ms  (JWT)                  │
│  ├─ Session Cache:          ~  10ms  (Redis)                │
│  ├─ Token Persistence:      ~  50ms  (PostgreSQL)           │
│  ├─ Last Login Update:      ~  30ms  (PostgreSQL)           │
│  └─ Audit Log:              ~ Async  (Non-blocking)         │
└──────────────────────────────────────────────────────────────┘

Database Operations: 3 queries (User SELECT, Token INSERT, User UPDATE)
Redis Operations:    5-6 commands
Total Round Trips:   8-9 network calls
```

---

**Legend:**
- ▶ : Data flow direction
- ├ : Connection/Branch
- └ : End of branch
- │ : Continuation
- ┌─┐ : Box boundaries
