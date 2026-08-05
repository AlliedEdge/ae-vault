# Ziboto Security Configuration

This document describes the security architecture and configuration for the Ziboto backend.

## Table of Contents
- [Overview](#overview)
- [Components](#components)
- [Authentication Flow](#authentication-flow)
- [Configuration](#configuration)
- [Endpoint Security](#endpoint-security)
- [Password Security](#password-security)
- [Testing](#testing)

---

## Overview

Ziboto uses **JWT-based stateless authentication** with Spring Security. This approach provides:

- ✅ **Stateless architecture** - No server-side sessions
- ✅ **Scalability** - Easy horizontal scaling
- ✅ **Mobile-friendly** - Works with any HTTP client
- ✅ **Microservice-ready** - JWT can be validated independently
- ✅ **Secure** - BCrypt password hashing, HS512 JWT signing

### Security Stack

- **Spring Security 6.x** - Security framework
- **JWT (JSON Web Tokens)** - Token-based authentication
- **BCrypt** - Password hashing
- **CORS** - Cross-origin resource sharing
- **HTTPS** - Transport layer security (production)

---

## Components

### 1. SecurityConfig

**Location:** `security/SecurityConfig.java`

Main Spring Security configuration class that sets up:

- HTTP security filter chain
- Authentication provider
- Password encoder
- Authentication manager
- CORS configuration
- CSRF protection (disabled for REST API)
- Session management (stateless)

**Key Features:**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,      // @Secured
    jsr250Enabled = true,        // @RolesAllowed
    prePostEnabled = true        // @PreAuthorize, @PostAuthorize
)
```

### 2. CustomUserDetailsService

**Location:** `auth/service/CustomUserDetailsService.java`

Loads user details from the database for authentication.

**Features:**
- Loads user by username or email
- Maps user roles to Spring Security authorities
- Enforces account status checks (ACTIVE, SUSPENDED, etc.)
- Provides detailed UserDetails for authentication

**Methods:**
- `loadUserByUsername(String usernameOrEmail)` - Load by username or email
- `loadUserById(Long userId)` - Load by user ID (for token refresh)

### 3. JwtAuthenticationFilter

**Location:** `security/JwtAuthenticationFilter.java`

Custom filter that processes JWT tokens on each request.

**Process:**
1. Extract JWT from Authorization header
2. Validate token signature and expiration
3. Extract username from token
4. Load user details from database
5. Set authentication in SecurityContext

### 4. JwtAuthenticationEntryPoint

**Location:** `security/JwtAuthenticationEntryPoint.java`

Handles authentication failures and returns JSON error responses.

**Error Types:**
- Token expired
- Invalid signature
- Malformed token
- Missing credentials

### 5. JwtTokenProvider

**Location:** `security/JwtTokenProvider.java`

Core JWT functionality for token generation and validation.

**Capabilities:**
- Generate access tokens (15 minutes)
- Generate refresh tokens (7 days)
- Validate token signatures
- Extract user information from tokens

### 6. BCrypt PasswordEncoder

**Bean:** `passwordEncoder()`

Secure password hashing with BCrypt algorithm.

**Features:**
- Default strength: 10 (2^10 = 1024 rounds)
- Built-in salt generation
- Resistant to rainbow table attacks
- Adaptive hashing (can increase strength over time)

---

## Authentication Flow

### Registration Flow

```
1. Client → POST /api/v1/auth/register
   Body: { username, email, password }

2. Backend validates input

3. Backend hashes password with BCrypt

4. Backend saves user to database

5. Backend generates JWT tokens

6. Backend → Client: { accessToken, refreshToken, user }
```

### Login Flow

```
1. Client → POST /api/v1/auth/login
   Body: { usernameOrEmail, password }

2. AuthenticationManager authenticates credentials

3. CustomUserDetailsService loads user from database

4. DaoAuthenticationProvider verifies password with BCrypt

5. JwtTokenProvider generates access and refresh tokens

6. Backend → Client: { accessToken, refreshToken, user }
```

### Authenticated Request Flow

```
1. Client → GET /api/v1/protected-resource
   Header: Authorization: Bearer {accessToken}

2. JwtAuthenticationFilter intercepts request

3. Filter extracts and validates JWT token

4. Filter loads user details from database

5. Filter sets authentication in SecurityContext

6. Request proceeds to controller

7. Controller processes request

8. Backend → Client: Response
```

### Token Refresh Flow

```
1. Client → POST /api/v1/auth/refresh
   Body: { refreshToken }

2. Backend validates refresh token

3. Backend generates new access token

4. Backend → Client: { accessToken, refreshToken }
```

---

## Configuration

### application.yml

```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET:}
      expiration: ${JWT_EXPIRATION:900000}           # 15 minutes
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 days
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
      allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
      allowed-headers: "*"
      allow-credentials: true
      max-age: 3600
```

### Environment Variables (.env)

```bash
# JWT Configuration
JWT_SECRET=your-base64-encoded-secret-key-here
JWT_EXPIRATION=900000            # 15 minutes in milliseconds
JWT_REFRESH_EXPIRATION=604800000 # 7 days in milliseconds

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

### Generate JWT Secret

```bash
# Generate a secure 256-bit secret key
./scripts/generate-jwt-secret.sh

# Or manually with OpenSSL
openssl rand -base64 32
```

---

## Endpoint Security

### Public Endpoints (No Authentication Required)

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/auth/register` | User registration |
| `POST /api/v1/auth/login` | User login |
| `POST /api/v1/auth/refresh` | Refresh access token |
| `GET /actuator/**` | Actuator endpoints |
| `GET /api-docs/**` | OpenAPI documentation |
| `GET /swagger-ui/**` | Swagger UI |

### Protected Endpoints (Authentication Required)

| Endpoint | Description | Authorization |
|----------|-------------|---------------|
| `GET /api/v1/user/me` | Current user profile | Authenticated user |
| `PUT /api/v1/user/me` | Update profile | Authenticated user |
| `GET /api/v1/files/**` | File operations | File owner or admin |
| `POST /api/v1/storage/**` | Storage operations | Authenticated user |

### Role-Based Authorization

Use method-level security annotations:

```java
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    
    // Only users with ROLE_ADMIN can access
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/users")
    public List<User> getAllUsers() {
        // ...
    }
    
    // Only ROLE_SUPER_ADMIN can access
    @Secured("ROLE_SUPER_ADMIN")
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        // ...
    }
    
    // Complex authorization logic
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.id")
    @GetMapping("/users/{userId}")
    public User getUser(@PathVariable Long userId) {
        // ...
    }
}
```

---

## Password Security

### BCrypt Configuration

**Default Strength:** 10 (2^10 = 1024 rounds)

| Strength | Rounds | Time (approx) | Use Case |
|----------|--------|---------------|----------|
| 10 | 1,024 | ~100ms | Development, Testing |
| 11 | 2,048 | ~200ms | Standard Production |
| 12 | 4,096 | ~400ms | High Security |
| 13 | 8,192 | ~800ms | Very High Security |

### Custom Strength

```java
// In SecurityConfig or separate configuration
@Bean
public PasswordEncoder customPasswordEncoder() {
    return SecurityConfig.passwordEncoder(12); // Strength 12
}
```

### Password Encoding Example

```java
@Autowired
private PasswordEncoder passwordEncoder;

// Encode password before saving
String rawPassword = "MySecurePassword123";
String encodedPassword = passwordEncoder.encode(rawPassword);

// Verify password during login
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

---

## Testing

### Testing with JWT Tokens

#### 1. Get JWT Token

```bash
# Login to get tokens
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john_doe",
    "password": "SecurePass123"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

#### 2. Use Token in Requests

```bash
# Use access token for authenticated requests
curl -X GET http://localhost:8080/api/v1/user/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

#### 3. Refresh Token

```bash
# Get new access token using refresh token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

### Testing with Spring Security Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Test
    void testAuthenticatedEndpoint() throws Exception {
        // Generate test token
        String token = tokenProvider.generateToken("john_doe", List.of("ROLE_USER"));
        
        // Make authenticated request
        mockMvc.perform(get("/api/v1/user/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
    
    @Test
    void testUnauthorizedAccess() throws Exception {
        // Request without token should fail
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

## Security Best Practices

### ✅ DO

- ✅ Store JWT secret in environment variables
- ✅ Use HTTPS in production
- ✅ Set appropriate token expiration times
- ✅ Validate tokens on every request
- ✅ Use strong password requirements
- ✅ Log authentication failures
- ✅ Implement rate limiting for auth endpoints
- ✅ Use secure password reset flows
- ✅ Enable CORS only for trusted origins
- ✅ Regularly rotate JWT secrets

### ❌ DON'T

- ❌ Store JWT secret in source code
- ❌ Use weak JWT secrets (< 256 bits)
- ❌ Set very long token expiration times
- ❌ Store sensitive data in JWT claims
- ❌ Use HTTP in production
- ❌ Disable CSRF for session-based auth
- ❌ Allow weak passwords
- ❌ Expose detailed error messages
- ❌ Use hardcoded credentials
- ❌ Trust client-side validation only

---

## Troubleshooting

### Common Issues

#### 1. "JWT secret is not configured"

**Problem:** JWT_SECRET environment variable not set

**Solution:**
```bash
# Generate and set JWT secret
./scripts/generate-jwt-secret.sh
export JWT_SECRET="your-generated-secret"
```

#### 2. "Invalid JWT signature"

**Problem:** Token signed with different secret or tampered

**Solution:** Ensure JWT_SECRET is consistent across all instances

#### 3. "Token has expired"

**Problem:** Access token expired (15 minutes)

**Solution:** Use refresh token to get new access token

#### 4. "User not found"

**Problem:** Username/email doesn't exist or account deleted

**Solution:** Check user exists and account status is ACTIVE

#### 5. "Account is locked"

**Problem:** User status is SUSPENDED

**Solution:** Admin must unsuspend the account

---

## References

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [JWT.io - JSON Web Tokens](https://jwt.io/)
- [BCrypt Calculator](https://bcrypt-generator.com/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
