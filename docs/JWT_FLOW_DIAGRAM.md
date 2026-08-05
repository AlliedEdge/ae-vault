# JWT Authentication Flow Diagrams

## Complete Request Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Client Application                            │
│                    (Browser, Mobile App, etc.)                        │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         │ 1. Login Request
                         │    POST /api/v1/auth/login
                         │    { username, password }
                         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Backend                           │
│                        (Port 8080)                                    │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         │ 2. AuthController receives request
                         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                          AuthService                                  │
│  • Validate credentials                                              │
│  • Check rate limits                                                 │
│  • Verify password (BCrypt)                                          │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         │ 3. Generate JWT Tokens
                         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       JwtTokenProvider                                │
│  • Access Token (15 min)                                             │
│  • Refresh Token (7 days)                                            │
│  • Sign with HS512                                                   │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         │ 4. Return tokens
                         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Client Application                            │
│  • Store access token in memory                                      │
│  • Store refresh token in secure storage                             │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         │ 5. API Request
                         │    GET /api/v1/users/profile
                         │    Authorization: Bearer <token>
                         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   JwtAuthenticationFilter                             │
│  • Extract token from header                                         │
│  • Validate signature                                                │
│  • Check expiration                                                  │
│  • Check blacklist                                                   │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                ┌────────┴────────┐
                │                 │
         Valid  │                 │  Invalid
                ▼                 ▼
    ┌──────────────────┐  ┌──────────────────┐
    │ Load UserDetails │  │ Return 401       │
    │ Populate Context │  │ Unauthorized     │
    └────────┬─────────┘  └──────────────────┘
             │
             │ 6. Execute Controller
             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      Protected Controller                             │
│  • User authenticated                                                │
│  • Roles available                                                   │
│  • Business logic                                                    │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         │ 7. Return response
                         ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Client Application                            │
│  • Display data                                                      │
│  • Handle errors                                                     │
└──────────────────────────────────────────────────────────────────────┘
```

## JWT Authentication Filter Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Incoming HTTP Request                              │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Is Public Path? │
                │ (/auth/login,   │
                │  /register,     │
                │  /swagger-ui)   │
                └────┬──────┬─────┘
                     │      │
                 YES │      │ NO
                     │      │
                     ▼      ▼
            ┌─────────┐  ┌──────────────────┐
            │  Skip   │  │ Check Auth       │
            │ Filter  │  │ Header           │
            └────┬────┘  └────┬─────────────┘
                 │            │
                 │            ▼
                 │    ┌────────────────┐
                 │    │ Has Bearer     │
                 │    │ Token?         │
                 │    └──┬────────┬────┘
                 │       │        │
                 │   YES │        │ NO
                 │       │        │
                 │       ▼        ▼
                 │  ┌────────┐  ┌──────────┐
                 │  │Extract │  │Continue  │
                 │  │Token   │  │No Auth   │
                 │  └───┬────┘  └────┬─────┘
                 │      │            │
                 │      ▼            │
                 │  ┌────────────┐  │
                 │  │ Validate   │  │
                 │  │ Token      │  │
                 │  └──┬────┬────┘  │
                 │     │    │        │
                 │ Valid│   │Invalid │
                 │     │    │        │
                 │     ▼    ▼        │
                 │  ┌─────┐ ┌──────┐│
                 │  │Check│ │Set   ││
                 │  │Black│ │Error ││
                 │  │list │ │Attr  ││
                 │  └──┬──┘ └──┬───┘│
                 │     │       │    │
                 │ Not │       │    │
                 │Black│       │    │
                 │list │       │    │
                 │     ▼       │    │
                 │  ┌────────┐ │    │
                 │  │Load    │ │    │
                 │  │User    │ │    │
                 │  └───┬────┘ │    │
                 │      │       │    │
                 │      ▼       │    │
                 │  ┌────────┐ │    │
                 │  │Create  │ │    │
                 │  │Auth    │ │    │
                 │  └───┬────┘ │    │
                 │      │       │    │
                 │      ▼       │    │
                 │  ┌────────┐ │    │
                 │  │Set     │ │    │
                 │  │Security│ │    │
                 │  │Context │ │    │
                 │  └───┬────┘ │    │
                 │      │       │    │
                 └──────┴───────┴────┘
                         │
                         ▼
             ┌───────────────────────┐
             │  Continue Filter Chain │
             │  (Next Filter/         │
             │   Controller)          │
             └───────────────────────┘
```

## Token Validation Process

```
┌──────────────────────────────────────────────────────────────────────┐
│                      JWT Token Validation                             │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Parse JWT       │
                │ Extract Claims  │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Verify          │
                │ Signature       │
                │ (HS512)         │
                └────┬──────┬─────┘
                     │      │
                Valid│      │Invalid
                     │      │
                     ▼      ▼
            ┌─────────┐  ┌──────────┐
            │Check    │  │Return    │
            │Expiry   │  │Invalid   │
            └────┬──┬─┘  └──────────┘
                 │  │
          Valid  │  │ Expired
                 │  │
                 ▼  ▼
        ┌─────────┐ ┌──────────┐
        │Check    │ │Return    │
        │Type     │ │Expired   │
        │(access) │ └──────────┘
        └────┬──┬─┘
             │  │
      Access │  │ Refresh
             │  │
             ▼  ▼
    ┌─────────┐ ┌──────────┐
    │Verify   │ │Return    │
    │Issuer & │ │Wrong Type│
    │Audience │ └──────────┘
    └────┬────┘
         │
         ▼
    ┌─────────┐
    │Token    │
    │Valid ✓  │
    └─────────┘
```

## SecurityContext Population

```
┌──────────────────────────────────────────────────────────────────────┐
│                    After Token Validation                             │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Extract         │
                │ Username        │
                │ from Token      │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Load User       │
                │ Details from    │
                │ Database        │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ UserDetails     │
                │ • username      │
                │ • authorities   │
                │ • enabled       │
                │ • locked        │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────────────────┐
                │ Create Authentication       │
                │ UsernamePasswordAuth        │
                │ • principal: UserDetails    │
                │ • credentials: null         │
                │ • authorities: roles        │
                └────────┬───────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Set Details     │
                │ • IP address    │
                │ • Session ID    │
                │ • Timestamp     │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────────────────┐
                │ SecurityContextHolder       │
                │ .getContext()               │
                │ .setAuthentication(auth)    │
                └────────┬───────────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │ SecurityContext Populated  │
            │ Available throughout       │
            │ request lifecycle          │
            └────────────────────────────┘
```

## Role-Based Authorization

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Controller Method Call                             │
│           @PreAuthorize("hasRole('ADMIN')")                          │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Get Current     │
                │ Authentication  │
                │ from Context    │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Extract         │
                │ Authorities     │
                │ (Roles)         │
                └────┬──────┬─────┘
                     │      │
                     │      │
            Has Role │      │ No Role
                     │      │
                     ▼      ▼
            ┌─────────┐  ┌──────────┐
            │Execute  │  │Throw     │
            │Method   │  │Access    │
            │         │  │Denied    │
            │         │  │Exception │
            └─────────┘  └────┬─────┘
                              │
                              ▼
                         ┌──────────┐
                         │Return    │
                         │403       │
                         │Forbidden │
                         └──────────┘
```

## Token Refresh Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Access Token Expires                               │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Client Detects  │
                │ 401 Response    │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ POST /refresh   │
                │ {refreshToken}  │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Validate        │
                │ Refresh Token   │
                └────┬──────┬─────┘
                     │      │
                Valid│      │Invalid
                     │      │
                     ▼      ▼
            ┌─────────┐  ┌──────────┐
            │Check DB │  │Return    │
            │Record   │  │401       │
            └────┬────┘  └──────────┘
                 │
            Found│
                 ▼
            ┌─────────┐
            │Check    │
            │Revoked &│
            │Expiry   │
            └────┬──┬─┘
                 │  │
            Valid│  │Invalid
                 │  │
                 ▼  ▼
        ┌─────────┐ ┌──────────┐
        │Generate │ │Return    │
        │New      │ │401       │
        │Tokens   │ └──────────┘
        └────┬────┘
             │
             ▼
        ┌─────────┐
        │Revoke   │
        │Old      │
        │Refresh  │
        └────┬────┘
             │
             ▼
        ┌─────────┐
        │Save New │
        │Refresh  │
        │Token    │
        └────┬────┘
             │
             ▼
        ┌─────────┐
        │Return   │
        │New      │
        │Tokens   │
        └─────────┘
```

## Logout Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    POST /api/v1/auth/logout                           │
│             Authorization: Bearer <access-token>                      │
└────────────────────────┬─────────────────────────────────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Extract Access  │
                │ Token from      │
                │ Header          │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Get Username    │
                │ from Security   │
                │ Context         │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────────────────┐
                │ TokenBlacklistService       │
                │ • Calculate remaining TTL   │
                │ • Store in Redis            │
                │ • Key: token:blacklist:JWT  │
                └────────┬───────────────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Find Active     │
                │ Refresh Tokens  │
                │ in Database     │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Mark All as     │
                │ Revoked         │
                │ (revoked=true)  │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Clear Session   │
                │ Cache in Redis  │
                │ (session:user:) │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Clear Active    │
                │ Sessions        │
                │ (session:active)│
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Create Audit    │
                │ Log (LOGOUT)    │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Return 200 OK   │
                │ "Logout         │
                │  successful"    │
                └─────────────────┘
```

## Component Interaction

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Request Processing                            │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │   JwtAuthenticationFilter      │
        │   • Extract token              │
        │   • Initial validation         │
        └────────┬───────────────────────┘
                 │
                 ├──────────────────────────────────┐
                 │                                  │
                 ▼                                  ▼
    ┌────────────────────┐            ┌────────────────────┐
    │  JwtTokenProvider  │            │ TokenBlacklist     │
    │  • Validate token  │            │ Service            │
    │  • Extract claims  │            │ • Check revoked    │
    │  • Check expiry    │            │ • Redis lookup     │
    └────────┬───────────┘            └────────┬───────────┘
             │                                  │
             └──────────┬───────────────────────┘
                        │
                        ▼
            ┌──────────────────────┐
            │ CustomUserDetails    │
            │ Service              │
            │ • Load from DB       │
            │ • Map authorities    │
            └──────────┬───────────┘
                       │
                       ▼
            ┌──────────────────────┐
            │ SecurityContext      │
            │ • Store auth         │
            │ • Make available     │
            └──────────┬───────────┘
                       │
                       ▼
            ┌──────────────────────┐
            │ Controller           │
            │ • Access user        │
            │ • Execute logic      │
            └──────────────────────┘
```

## Data Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│          │     │          │     │          │     │          │
│  Client  │────▶│  Spring  │────▶│ Database │     │  Redis   │
│          │     │  Boot    │     │ (Postgres│     │  Cache   │
│          │◀────│          │◀────│  SQL)    │     │          │
└──────────┘     └────┬─────┘     └──────────┘     └────┬─────┘
                      │                                   │
                      │                                   │
                      └───────────────┬───────────────────┘
                                      │
                      ┌───────────────▼──────────────┐
                      │                              │
                      │  JWT Token Operations:       │
                      │  • Validation (in-memory)    │
                      │  • Blacklist check (Redis)   │
                      │  • User load (DB/Redis)      │
                      │  • Session cache (Redis)     │
                      │                              │
                      └──────────────────────────────┘
```

## Legend

```
┌─────────┐
│ Process │  - Processing step or component
└─────────┘

┌─────────┐
│Decision?│  - Decision point
└───┬───┬─┘
    │   │

────▶      - Data flow direction

├──────    - Branch/Split

└──────    - Merge/Join
```
