# Redis Architecture Diagram

## System Overview

```mermaid
graph TB
    subgraph "Application Layer"
        AuthController[Auth Controller]
        UserController[User Controller]
        APIController[API Controllers]
    end
    
    subgraph "Service Layer"
        RateLimitService[Rate Limit Service]
        FailedLoginService[Failed Login Service]
        SessionCacheService[Session Cache Service]
        TokenBlacklistService[Token Blacklist Service]
        OtpCacheService[OTP Cache Service]
    end
    
    subgraph "Redis Abstraction Layer"
        RedisService[Redis Service<br/>Centralized Operations]
        RedisProperties[Redis Properties<br/>Configuration]
    end
    
    subgraph "Spring Data Redis"
        RedisTemplate[Redis Template]
        RedisConfig[Redis Config]
        ConnectionFactory[Connection Factory]
    end
    
    subgraph "Infrastructure"
        Redis[(Redis Server)]
    end
    
    AuthController --> RateLimitService
    AuthController --> FailedLoginService
    AuthController --> SessionCacheService
    AuthController --> TokenBlacklistService
    AuthController --> OtpCacheService
    
    UserController --> SessionCacheService
    APIController --> RateLimitService
    
    RateLimitService --> RedisService
    FailedLoginService --> RedisService
    SessionCacheService --> RedisService
    TokenBlacklistService --> RedisService
    OtpCacheService --> RedisService
    
    RedisService --> RedisTemplate
    RedisService -.reads config.-> RedisProperties
    
    RateLimitService -.reads config.-> RedisProperties
    FailedLoginService -.reads config.-> RedisProperties
    SessionCacheService -.reads config.-> RedisProperties
    TokenBlacklistService -.reads config.-> RedisProperties
    OtpCacheService -.reads config.-> RedisProperties
    
    RedisTemplate --> ConnectionFactory
    RedisConfig --> RedisTemplate
    RedisConfig --> ConnectionFactory
    
    ConnectionFactory --> Redis
    
    style RedisService fill:#4CAF50
    style RedisProperties fill:#2196F3
    style Redis fill:#DC382D
```

## Data Flow

### Login Flow with Rate Limiting and Failed Attempt Tracking

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant RateLimitService
    participant FailedLoginService
    participant AuthService
    participant SessionCache
    participant Redis
    
    Client->>AuthController: POST /auth/login
    
    AuthController->>RateLimitService: isLoginRateLimitExceeded(username)
    RateLimitService->>Redis: GET rate_limit:login:username
    Redis-->>RateLimitService: count
    alt Rate limit exceeded
        RateLimitService-->>AuthController: true
        AuthController-->>Client: 429 Too Many Requests
    else Rate limit OK
        RateLimitService-->>AuthController: false
        
        AuthController->>FailedLoginService: isLocked(username)
        FailedLoginService->>Redis: EXISTS failed_login:lockout:username
        Redis-->>FailedLoginService: exists
        alt Account locked
            FailedLoginService-->>AuthController: true
            AuthController-->>Client: 423 Locked
        else Not locked
            FailedLoginService-->>AuthController: false
            
            AuthController->>RateLimitService: recordLoginAttempt(username)
            RateLimitService->>Redis: INCR rate_limit:login:username
            Redis-->>RateLimitService: new_count
            
            AuthController->>AuthService: authenticate(credentials)
            
            alt Authentication failed
                AuthService-->>AuthController: failure
                AuthController->>FailedLoginService: recordFailedAttempt(username)
                FailedLoginService->>Redis: INCR failed_login:attempts:username
                Redis-->>FailedLoginService: attempt_count
                alt Max attempts reached
                    FailedLoginService->>Redis: SET failed_login:lockout:username
                end
                AuthController-->>Client: 401 Unauthorized
            else Authentication successful
                AuthService-->>AuthController: user
                AuthController->>RateLimitService: resetLoginRateLimit(username)
                RateLimitService->>Redis: DEL rate_limit:login:username
                
                AuthController->>FailedLoginService: resetFailedAttempts(username)
                FailedLoginService->>Redis: DEL failed_login:*:username
                
                AuthController->>SessionCache: cacheUserSession(username, user)
                SessionCache->>Redis: SET session:user:username
                
                AuthController-->>Client: 200 OK + tokens
            end
        end
    end
```

### OTP Verification Flow

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant OtpCacheService
    participant EmailService
    participant Redis
    
    Client->>Controller: POST /auth/request-otp
    
    Controller->>OtpCacheService: generateOtp(email, EMAIL_VERIFICATION)
    
    OtpCacheService->>Redis: GET otp:rate:email_verification:email
    Redis-->>OtpCacheService: generation_count
    
    alt Rate limited
        OtpCacheService-->>Controller: null
        Controller-->>Client: 429 Too Many Requests
    else OK to generate
        OtpCacheService->>OtpCacheService: generateSecureOtp()
        OtpCacheService->>Redis: SET otp:otp:email_verification:email
        OtpCacheService->>Redis: HMSET otp:meta:email_verification:email
        OtpCacheService->>Redis: INCR otp:rate:email_verification:email
        Redis-->>OtpCacheService: otp
        
        OtpCacheService-->>Controller: otp
        Controller->>EmailService: sendOtpEmail(email, otp)
        Controller-->>Client: 200 OK
        
        Note over Client: User receives email with OTP
        
        Client->>Controller: POST /auth/verify-otp {otp}
        
        Controller->>OtpCacheService: verifyOtp(email, otp, EMAIL_VERIFICATION)
        OtpCacheService->>Redis: GET otp:otp:email_verification:email
        Redis-->>OtpCacheService: stored_otp
        
        OtpCacheService->>Redis: HINCRBY otp:meta:email_verification:email attempts
        
        alt Invalid OTP
            OtpCacheService-->>Controller: false
            Controller-->>Client: 400 Invalid OTP
        else Valid OTP
            OtpCacheService->>Redis: DEL otp:*:email_verification:email
            OtpCacheService-->>Controller: true
            Controller-->>Client: 200 OK
        end
    end
```

## Redis Key Structure

```mermaid
graph LR
    subgraph "Rate Limiting Keys"
        RL1[rate_limit:login:identifier]
        RL2[rate_limit:signup:identifier]
        RL3[rate_limit:api:identifier]
        RL4[rate_limit:refresh:identifier]
    end
    
    subgraph "Failed Login Keys"
        FL1[failed_login:attempts:identifier]
        FL2[failed_login:lockout:identifier]
        FL3[failed_login:last:identifier]
    end
    
    subgraph "Session Keys"
        S1[session:user:username]
        S2[session:meta:session_id]
        S3[session:active:username]
    end
    
    subgraph "Token Keys"
        T1[token:blacklist:token:token_value]
        T2[token:blacklist:user:username]
    end
    
    subgraph "OTP Keys"
        O1[otp:otp:purpose:identifier]
        O2[otp:meta:purpose:identifier]
        O3[otp:rate:purpose:identifier]
    end
    
    style RL1 fill:#FFC107
    style RL2 fill:#FFC107
    style RL3 fill:#FFC107
    style RL4 fill:#FFC107
    style FL1 fill:#F44336
    style FL2 fill:#F44336
    style FL3 fill:#F44336
    style S1 fill:#4CAF50
    style S2 fill:#4CAF50
    style S3 fill:#4CAF50
    style T1 fill:#9C27B0
    style T2 fill:#9C27B0
    style O1 fill:#2196F3
    style O2 fill:#2196F3
    style O3 fill:#2196F3
```

## Configuration Flow

```mermaid
graph TD
    ENV[Environment Variables<br/>.env file] --> YML[application.yml]
    YML --> RP[RedisProperties<br/>@ConfigurationProperties]
    
    RP --> RLS[RateLimitService]
    RP --> FLS[FailedLoginService]
    RP --> SCS[SessionCacheService]
    RP --> TBS[TokenBlacklistService]
    RP --> OCS[OtpCacheService]
    
    RLS --> RS[RedisService]
    FLS --> RS
    SCS --> RS
    TBS --> RS
    OCS --> RS
    
    RS --> RT[RedisTemplate]
    RT --> REDIS[(Redis)]
    
    style ENV fill:#FFE082
    style RP fill:#2196F3
    style RS fill:#4CAF50
    style REDIS fill:#DC382D
```

## Service Responsibilities

```mermaid
graph TB
    subgraph "Rate Limiting"
        R1[Login Rate Limit]
        R2[Signup Rate Limit]
        R3[API Rate Limit]
        R4[Token Refresh Rate Limit]
    end
    
    subgraph "Security"
        S1[Failed Login Tracking]
        S2[Account Lockout]
        S3[Token Blacklisting]
        S4[OTP Generation & Verification]
    end
    
    subgraph "Performance"
        P1[User Session Cache]
        P2[Session Metadata]
        P3[Active Session Tracking]
    end
    
    subgraph "Core Operations"
        C1[Set/Get with TTL]
        C2[Increment/Decrement]
        C3[Hash Operations]
        C4[Set Operations]
    end
    
    R1 --> C2
    R2 --> C2
    R3 --> C2
    R4 --> C2
    
    S1 --> C2
    S2 --> C1
    S3 --> C1
    S4 --> C1
    S4 --> C3
    
    P1 --> C1
    P2 --> C3
    P3 --> C3
    
    style R1 fill:#FFC107
    style R2 fill:#FFC107
    style R3 fill:#FFC107
    style R4 fill:#FFC107
    style S1 fill:#F44336
    style S2 fill:#F44336
    style S3 fill:#9C27B0
    style S4 fill:#2196F3
    style P1 fill:#4CAF50
    style P2 fill:#4CAF50
    style P3 fill:#4CAF50
```

## Deployment Architecture

```mermaid
graph TB
    subgraph "Application Servers"
        APP1[Ziboto Backend 1]
        APP2[Ziboto Backend 2]
        APP3[Ziboto Backend N]
    end
    
    subgraph "Redis Layer"
        subgraph "Redis Sentinel (HA)"
            SENTINEL1[Sentinel 1]
            SENTINEL2[Sentinel 2]
            SENTINEL3[Sentinel 3]
        end
        
        MASTER[(Redis Master)]
        REPLICA1[(Redis Replica 1)]
        REPLICA2[(Redis Replica 2)]
    end
    
    subgraph "Monitoring"
        METRICS[Metrics Collection]
        ALERTS[Alerting]
    end
    
    APP1 --> SENTINEL1
    APP2 --> SENTINEL2
    APP3 --> SENTINEL3
    
    SENTINEL1 -.monitors.-> MASTER
    SENTINEL2 -.monitors.-> MASTER
    SENTINEL3 -.monitors.-> MASTER
    
    MASTER -.replicates.-> REPLICA1
    MASTER -.replicates.-> REPLICA2
    
    SENTINEL1 -.monitors.-> REPLICA1
    SENTINEL1 -.monitors.-> REPLICA2
    
    MASTER --> METRICS
    REPLICA1 --> METRICS
    REPLICA2 --> METRICS
    
    METRICS --> ALERTS
    
    style MASTER fill:#DC382D
    style REPLICA1 fill:#FF8A80
    style REPLICA2 fill:#FF8A80
    style APP1 fill:#4CAF50
    style APP2 fill:#4CAF50
    style APP3 fill:#4CAF50
```

## Error Handling Flow

```mermaid
graph TD
    START[Redis Operation] --> TRY{Try Operation}
    
    TRY -->|Success| LOG_SUCCESS[Log Debug]
    TRY -->|Exception| CATCH[Catch Exception]
    
    LOG_SUCCESS --> RETURN_SUCCESS[Return Result]
    
    CATCH --> LOG_ERROR[Log Error with Context]
    LOG_ERROR --> FAIL_STRATEGY{Fail Strategy}
    
    FAIL_STRATEGY -->|Fail Open| RETURN_DEFAULT[Return Default/Null]
    FAIL_STRATEGY -->|Fail Closed| THROW[Throw Exception]
    
    RETURN_DEFAULT --> APP_CONTINUES[Application Continues]
    THROW --> APP_HANDLES[Application Handles Error]
    
    style TRY fill:#2196F3
    style CATCH fill:#F44336
    style LOG_ERROR fill:#FF9800
    style APP_CONTINUES fill:#4CAF50
```

## Memory Management

```mermaid
graph TB
    subgraph "TTL Strategy"
        T1[Rate Limit Counters<br/>Window-based TTL]
        T2[Failed Login Data<br/>1 hour TTL]
        T3[Session Cache<br/>1-24 hour TTL]
        T4[Token Blacklist<br/>Token expiry TTL]
        T5[OTP Data<br/>5 minute TTL]
    end
    
    subgraph "Redis Memory"
        MEMORY[(Redis Memory)]
    end
    
    subgraph "Eviction"
        E1[Automatic Expiration]
        E2[LRU Eviction<br/>if maxmemory reached]
    end
    
    T1 --> MEMORY
    T2 --> MEMORY
    T3 --> MEMORY
    T4 --> MEMORY
    T5 --> MEMORY
    
    MEMORY --> E1
    MEMORY --> E2
    
    E1 --> FREE[Free Memory]
    E2 --> FREE
    
    style MEMORY fill:#DC382D
    style E1 fill:#4CAF50
    style E2 fill:#FF9800
    style FREE fill:#4CAF50
```

## Summary

This architecture provides:

1. **Separation of Concerns:** Each service has a specific responsibility
2. **Centralized Operations:** All Redis operations through RedisService
3. **Centralized Configuration:** All settings in RedisProperties
4. **High Availability:** Support for Redis Sentinel/Cluster
5. **Fail-Safe:** Graceful degradation on Redis failures
6. **Performance:** Efficient caching and rate limiting
7. **Security:** Token blacklisting, rate limiting, account lockout
8. **Scalability:** Horizontal scaling support
9. **Monitoring:** Built-in metrics and logging
10. **Maintainability:** Clean, documented, testable code
