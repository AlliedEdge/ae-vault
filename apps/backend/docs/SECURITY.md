# Security Implementation Guide

## Overview

This document describes the comprehensive production-grade security features implemented in the Ziboto Backend application.

## Table of Contents

1. [Security Features](#security-features)
2. [Authentication & Authorization](#authentication--authorization)
3. [Rate Limiting](#rate-limiting)
4. [Account Locking](#account-locking)
5. [Security Headers](#security-headers)
6. [CORS Configuration](#cors-configuration)
7. [Request Validation](#request-validation)
8. [Error Handling](#error-handling)
9. [Audit Logging](#audit-logging)
10. [Configuration](#configuration)
11. [Security Best Practices](#security-best-practices)

---

## Security Features

### ✅ Implemented Features

- **JWT-based Authentication** - Stateless token authentication with access and refresh tokens
- **Rate Limiting** - Redis-based rate limiting for login, signup, API, and token refresh
- **Account Locking** - Automatic account lockout after failed login attempts
- **Security Headers** - Comprehensive HTTP security headers (CSP, HSTS, X-Frame-Options, etc.)
- **CORS Protection** - Configurable cross-origin resource sharing
- **Request Validation** - Bean validation with standardized error messages
- **Global Exception Handling** - Centralized error handling with security logging
- **Audit Logging** - Comprehensive audit trail for security events
- **Token Blacklisting** - JWT revocation for logout and security events
- **Session Management** - Redis-based session caching with sliding window expiration
- **Password Security** - BCrypt hashing with configurable strength
- **Input Validation** - Comprehensive validation messages and constraints

---

## Authentication & Authorization

### JWT Token Flow

```
┌─────────────┐           ┌──────────────┐           ┌──────────────┐
│   Client    │           │   Backend    │           │  PostgreSQL  │
└──────┬──────┘           └──────┬───────┘           └──────┬───────┘
       │                          │                          │
       │  1. POST /auth/login     │                          │
       │  (username, password)    │                          │
       ├─────────────────────────>│                          │
       │                          │  2. Verify credentials   │
       │                          ├─────────────────────────>│
       │                          │<─────────────────────────┤
       │                          │  3. Generate JWT tokens  │
       │  4. Return tokens        │     (access + refresh)   │
       │<─────────────────────────┤                          │
       │                          │                          │
       │  5. Request with token   │                          │
       │  Authorization: Bearer   │                          │
       ├─────────────────────────>│  6. Validate token       │
       │                          │  7. Check blacklist      │
       │  8. Return response      │                          │
       │<─────────────────────────┤                          │
```

### Token Types

#### Access Token
- **Duration**: 15 minutes (configurable)
- **Purpose**: API authentication
- **Storage**: Client-side (memory or secure storage)
- **Claims**: username, roles, type=access

#### Refresh Token
- **Duration**: 7 days (configurable)
- **Purpose**: Obtaining new access tokens
- **Storage**: Database (hashed) + Client-side
- **Claims**: username, type=refresh
- **Rotation**: New refresh token issued on each use

### Authentication Endpoints

| Endpoint | Method | Description | Rate Limit |
|----------|--------|-------------|------------|
| `/api/v1/auth/register` | POST | Register new user | 3 per hour |
| `/api/v1/auth/login` | POST | Authenticate user | 5 per 15 min |
| `/api/v1/auth/refresh` | POST | Refresh access token | 10 per hour |
| `/api/v1/auth/logout` | POST | Logout and revoke tokens | - |
| `/api/v1/auth/verify` | GET | Verify token validity | - |

---

## Rate Limiting

### Overview

Rate limiting is implemented using Redis with token bucket algorithm to prevent:
- Brute force attacks
- Spam registrations
- API abuse
- Token refresh abuse

### Rate Limit Configuration

```yaml
app:
  redis:
    rate-limit:
      login:
        max-attempts: 5
        window-minutes: 15
      signup:
        max-attempts: 3
        window-minutes: 60
      api:
        max-requests: 100
        window-minutes: 1
      refresh:
        max-attempts: 10
        window-hours: 1
```

### Rate Limit Responses

When rate limit is exceeded:
- **HTTP Status**: `429 Too Many Requests`
- **Header**: `Retry-After: 60` (seconds)
- **Error Code**: `6000-6004` (specific to limit type)

Example response:
```json
{
  "success": false,
  "message": "Too many login attempts. Please try again in 900 seconds.",
  "timestamp": "2024-01-15T10:30:45"
}
```

---

## Account Locking

### Failed Login Attempt Tracking

Account locking prevents brute force attacks by temporarily locking accounts after multiple failed login attempts.

### Configuration

```yaml
app:
  redis:
    failed-login:
      max-attempts: 5
      lockout-minutes: 30
      tracking-hours: 1
```

### Lockout Process

1. **Failed Login**: Password verification fails
2. **Attempt Recorded**: Counter incremented in Redis
3. **Threshold Check**: If attempts ≥ max-attempts
4. **Account Locked**: User cannot login for lockout period
5. **Auto-Unlock**: Automatic unlock after lockout period expires

### Lockout Response

```json
{
  "success": false,
  "message": "Account is locked due to multiple failed login attempts. Please try again in 1800 seconds.",
  "timestamp": "2024-01-15T10:30:45"
}
```

### Manual Unlock

Administrators can manually unlock accounts using:
```java
failedLoginAttemptService.unlockAccount(username);
```

---

## Security Headers

### Implemented Headers

The `SecurityHeadersFilter` adds the following security headers to all responses:

#### 1. X-Content-Type-Options
```
X-Content-Type-Options: nosniff
```
**Purpose**: Prevents MIME type sniffing attacks

#### 2. X-Frame-Options
```
X-Frame-Options: DENY
```
**Purpose**: Prevents clickjacking by disallowing iframe embedding

#### 3. X-XSS-Protection
```
X-XSS-Protection: 1; mode=block
```
**Purpose**: Enables XSS filter in legacy browsers

#### 4. Strict-Transport-Security (HSTS)
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```
**Purpose**: Enforces HTTPS connections (production only)

#### 5. Content-Security-Policy (CSP)
```
Content-Security-Policy: default-src 'self'; script-src 'self'; ...
```
**Purpose**: Restricts resource loading to prevent XSS and injection attacks

#### 6. Referrer-Policy
```
Referrer-Policy: strict-origin-when-cross-origin
```
**Purpose**: Controls referrer information leakage

#### 7. Permissions-Policy
```
Permissions-Policy: geolocation=(), microphone=(), camera=(), ...
```
**Purpose**: Disables unnecessary browser features

#### 8. Cache-Control (Sensitive Endpoints)
```
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
```
**Purpose**: Prevents caching of sensitive data

### CSP Configuration

The default Content Security Policy is strict. Adjust based on your frontend requirements:

```java
// In SecurityHeadersFilter.java
private String buildContentSecurityPolicy() {
    return String.join("; ",
        "default-src 'self'",
        "script-src 'self'",        // Add trusted domains if needed
        "style-src 'self' 'unsafe-inline'",
        // ... other directives
    );
}
```

---

## CORS Configuration

### Overview

Cross-Origin Resource Sharing (CORS) is configured to allow specific origins to access the API.

### Configuration

```yaml
app:
  security:
    cors:
      allowed-origins: http://localhost:5173,https://app.ziboto.com
      allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
      allowed-headers: "*"
      allow-credentials: true
      max-age: 3600
```

### Environment-Specific Configuration

**Development**:
```yaml
# application-dev.yml
app:
  security:
    cors:
      allowed-origins: http://localhost:5173,http://localhost:3000
```

**Production**:
```yaml
# application-prod.yml
app:
  security:
    cors:
      allowed-origins: https://app.ziboto.com
```

---

## Request Validation

### Validation Architecture

```
┌──────────────┐
│   Request    │
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│  Controller      │
│  @Valid          │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  Bean Validation │
│  (JSR-380)       │
└──────┬───────────┘
       │
       ├─── Valid ──────────────────────────┐
       │                                    ▼
       │                          ┌─────────────────┐
       │                          │  Process Request│
       │                          └─────────────────┘
       │
       └─── Invalid ────────────────────────┐
                                            ▼
                                   ┌─────────────────────┐
                                   │ GlobalExceptionHandler│
                                   │ Return 400 + errors  │
                                   └─────────────────────┘
```

### Validation Messages

Centralized validation messages in `ValidationMessages.java`:

```java
public static final String USERNAME_REQUIRED = "Username is required";
public static final String EMAIL_INVALID = "Email address is not valid";
public static final String PASSWORD_STRENGTH = "Password must contain at least one uppercase letter, one lowercase letter, and one number";
```

### Example DTO

```java
public class RegisterRequest {
    @NotBlank(message = ValidationMessages.USERNAME_REQUIRED)
    @Size(min = 3, max = 50, message = ValidationMessages.USERNAME_SIZE)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = ValidationMessages.USERNAME_PATTERN)
    private String username;
    
    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @Email(message = ValidationMessages.EMAIL_INVALID)
    private String email;
    
    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @Size(min = 8, max = 100, message = ValidationMessages.PASSWORD_SIZE)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
            message = ValidationMessages.PASSWORD_STRENGTH)
    private String password;
}
```

### Validation Error Response

```json
{
  "success": false,
  "message": "Validation failed for one or more fields",
  "errors": {
    "username": "Username must be between 3 and 50 characters",
    "email": "Email address is not valid",
    "password": "Password must contain at least one uppercase letter, one lowercase letter, and one number"
  },
  "timestamp": "2024-01-15T10:30:45"
}
```

---

## Error Handling

### Error Code Structure

Error codes are organized by category with numeric ranges:

| Range | Category | Examples |
|-------|----------|----------|
| 1000-1099 | General | Validation, Not Found, Server Errors |
| 2000-2099 | Authentication | Invalid Credentials, Token Errors |
| 3000-3099 | User | User Not Found, Email/Username Taken |
| 4000-4099 | Storage/Files | Upload Failed, File Too Large |
| 5000-5099 | Buckets | Bucket Not Found, Quota Exceeded |
| 6000-6099 | Rate Limiting | Login Rate Limit, API Rate Limit |
| 7000-7099 | Sessions/Tokens | Session Expired, Token Rotation Failed |

### Error Response Format

```json
{
  "success": false,
  "message": "Error message",
  "data": null,
  "errors": { /* optional field-level errors */ },
  "timestamp": "2024-01-15T10:30:45"
}
```

### Custom Exceptions

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `RateLimitExceededException` | 429 | Rate limit exceeded |
| `AccountLockedException` | 403 | Account locked |
| `InvalidTokenException` | 401 | Invalid JWT token |
| `UnauthorizedException` | 401 | Authentication required |
| `ConflictException` | 409 | Resource conflict |
| `ValidationException` | 400 | Validation failed |
| `ResourceNotFoundException` | 404 | Resource not found |

---

## Audit Logging

### Audit Log Entity

```java
@Entity
public class AuditLog {
    private User user;           // User performing action
    private String entityType;   // Entity affected (User, File, Bucket)
    private Long entityId;       // ID of affected entity
    private AuditAction action;  // Action performed
    private String details;      // Additional details (JSON)
    private String ipAddress;    // Client IP address
    private String userAgent;    // Browser/client info
    private LocalDateTime createdAt;
}
```

### Audit Actions

```java
public enum AuditAction {
    // Authentication
    LOGIN, LOGOUT, TOKEN_REFRESH,
    
    // Data Operations
    CREATE, READ, UPDATE, DELETE,
    
    // File Operations
    FILE_UPLOAD, FILE_DOWNLOAD, FILE_DELETE, FILE_SHARE,
    
    // User Management
    USER_CREATE, USER_UPDATE, USER_DELETE, PASSWORD_CHANGE,
    
    // Administrative
    PERMISSION_CHANGE, ROLE_CHANGE, CONFIG_CHANGE
}
```

### Creating Audit Logs

```java
// Automatic logging (async)
auditService.log(
    userId,
    "User",
    userId,
    AuditAction.LOGIN,
    String.format("Successful login from IP: %s", ipAddress)
);
```

### Audit Log Files

Separate log files are maintained:
- `ziboto.log` - General application logs
- `ziboto-security.log` - Security events (login, logout, failures)
- `ziboto-audit.log` - Audit trail (data access, changes)
- `ziboto-error.log` - Error logs

---

## Configuration

### Environment Variables

Required environment variables:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5433/ziboto
DATABASE_USERNAME=ziboto
DATABASE_PASSWORD=<secure-password>

# Redis
REDIS_HOST=localhost
REDIS_PORT=6380
REDIS_PASSWORD=<secure-password>

# JWT
JWT_SECRET=<base64-encoded-secret-256-bits-minimum>
JWT_EXPIRATION=900000          # 15 minutes
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173

# Active Profile
SPRING_PROFILES_ACTIVE=prod
```

### Generating JWT Secret

```bash
# Generate a secure 256-bit secret
openssl rand -base64 32

# Or use the provided script
./scripts/generate-jwt-secret.sh
```

### Redis Configuration

Redis is used for:
- Rate limiting
- Failed login attempt tracking
- Session caching
- Token blacklisting
- OTP caching (future)

### Application Profiles

| Profile | Purpose | Logging | Security Headers |
|---------|---------|---------|------------------|
| dev | Development | DEBUG | HSTS disabled |
| test | Testing | INFO | HSTS disabled |
| prod | Production | WARN/INFO | HSTS enabled |

---

## Security Best Practices

### 1. JWT Token Management

✅ **Do:**
- Store access tokens in memory (not localStorage)
- Use HTTP-only cookies for refresh tokens (if possible)
- Implement token rotation on refresh
- Set appropriate expiration times
- Blacklist tokens on logout

❌ **Don't:**
- Store tokens in localStorage (XSS vulnerability)
- Use same token for extended periods
- Share tokens between users
- Log token values

### 2. Password Security

✅ **Do:**
- Use BCrypt with strength ≥ 10
- Enforce password complexity requirements
- Implement password history
- Rate limit password reset attempts
- Use secure password reset tokens

❌ **Don't:**
- Store passwords in plain text
- Use MD5 or SHA1 for passwords
- Allow common passwords
- Send passwords via email
- Implement custom encryption

### 3. API Security

✅ **Do:**
- Validate all input data
- Use HTTPS in production
- Implement rate limiting
- Log security events
- Use parameterized queries
- Sanitize user input

❌ **Don't:**
- Trust client-side validation only
- Expose stack traces in responses
- Return detailed error messages in production
- Allow unbounded requests
- Use string concatenation for SQL

### 4. Session Management

✅ **Do:**
- Use secure, HTTP-only cookies
- Implement session timeout
- Track active sessions
- Invalidate sessions on logout
- Use sliding window expiration

❌ **Don't:**
- Allow unlimited concurrent sessions
- Store sensitive data in sessions
- Use predictable session IDs
- Allow session fixation
- Forget to invalidate old sessions

### 5. Logging and Monitoring

✅ **Do:**
- Log all security events
- Monitor failed login attempts
- Track suspicious activity
- Rotate log files
- Secure log file access

❌ **Don't:**
- Log sensitive data (passwords, tokens)
- Ignore log file growth
- Allow public access to logs
- Skip log analysis
- Disable security logging

---

## Security Checklist

### Pre-Production

- [ ] Change default JWT secret
- [ ] Configure strong database passwords
- [ ] Set up Redis with password protection
- [ ] Configure CORS for production domains
- [ ] Enable HSTS in production
- [ ] Review and adjust CSP policies
- [ ] Set up log monitoring and alerts
- [ ] Configure backup strategy for audit logs
- [ ] Test rate limiting thresholds
- [ ] Verify account lockout works correctly
- [ ] Test token blacklisting
- [ ] Review error messages for information disclosure
- [ ] Enable production logging profile
- [ ] Set up security monitoring dashboard

### Post-Deployment

- [ ] Monitor failed login attempts
- [ ] Review audit logs regularly
- [ ] Check rate limiting effectiveness
- [ ] Monitor token expiration issues
- [ ] Review security headers with security scanner
- [ ] Test CORS configuration
- [ ] Verify HTTPS certificate
- [ ] Test backup and recovery procedures
- [ ] Review and rotate secrets regularly
- [ ] Update dependencies for security patches

---

## Troubleshooting

### Common Issues

#### 1. CORS Errors

**Symptom**: Browser console shows CORS policy errors

**Solution**:
```yaml
# Add your frontend domain to allowed origins
app:
  security:
    cors:
      allowed-origins: http://localhost:5173,https://yourdomain.com
```

#### 2. Rate Limit False Positives

**Symptom**: Legitimate users hitting rate limits

**Solution**:
```yaml
# Increase rate limits or window duration
app:
  redis:
    rate-limit:
      login:
        max-attempts: 10  # Increase from 5
        window-minutes: 15
```

#### 3. Token Expiration Issues

**Symptom**: Users frequently logged out

**Solution**:
```yaml
# Increase token expiration
app:
  security:
    jwt:
      expiration: 1800000  # 30 minutes instead of 15
```

#### 4. Account Lockout Issues

**Symptom**: Users locked out unnecessarily

**Solution**:
```bash
# Manually unlock account
curl -X POST http://localhost:8080/api/v1/admin/unlock-account \
  -H "Authorization: Bearer <admin-token>" \
  -d '{"username":"user@example.com"}'
```

---

## Security Contact

For security issues or vulnerabilities, please contact:
- **Email**: security@ziboto.com
- **Response Time**: Within 24 hours

**Do not** publicly disclose security vulnerabilities before they are addressed.

---

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [CORS Specification](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)

---

**Last Updated**: January 2024  
**Version**: 1.0.0
