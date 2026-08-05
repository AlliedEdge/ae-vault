# Frontend Integration Requirements

This document specifies the exact API contract expected by the frontend for successful integration.

## Base URL

All endpoints must be prefixed with: `/api/v1`

Example: `http://localhost:8080/api/v1/auth/login`

## Required Endpoints

### 1. Register User

**Endpoint**: `POST /api/v1/auth/register`

**Request Body**:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123"
}
```

**Success Response** (200 OK):
```json
{
  "user": {
    "id": "uuid-string",
    "email": "john@example.com",
    "name": "John Doe",
    "role": "USER",
    "emailVerified": false,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error Response** (400 Bad Request):
```json
{
  "message": "Validation failed",
  "statusCode": 400,
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/auth/register",
  "validationErrors": [
    {
      "field": "email",
      "message": "Email is already in use"
    }
  ]
}
```

---

### 2. Login User

**Endpoint**: `POST /api/v1/auth/login`

**Request Body**:
```json
{
  "email": "john@example.com",
  "password": "SecurePass123",
  "rememberMe": false
}
```

**Success Response** (200 OK):
```json
{
  "user": {
    "id": "uuid-string",
    "email": "john@example.com",
    "name": "John Doe",
    "role": "USER",
    "emailVerified": true,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error Response** (401 Unauthorized):
```json
{
  "message": "Invalid email or password",
  "statusCode": 401,
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/auth/login"
}
```

---

### 3. Refresh Token

**Endpoint**: `POST /api/v1/auth/refresh`

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Success Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error Response** (401 Unauthorized):
```json
{
  "message": "Invalid or expired refresh token",
  "statusCode": 401,
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/auth/refresh"
}
```

---

### 4. Logout User

**Endpoint**: `POST /api/v1/auth/logout`

**Headers**:
```
Authorization: Bearer <access-token>
```

**Request Body**: None (or empty object)

**Success Response** (200 OK or 204 No Content):
```json
{}
```

**Note**: Frontend will clear tokens locally regardless of backend response.

---

### 5. Get Current User Profile

**Endpoint**: `GET /api/v1/users/me`

**Headers**:
```
Authorization: Bearer <access-token>
```

**Success Response** (200 OK):
```json
{
  "id": "uuid-string",
  "email": "john@example.com",
  "name": "John Doe",
  "role": "USER",
  "emailVerified": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "phoneNumber": "+1234567890",
  "avatarUrl": "https://example.com/avatar.jpg",
  "bio": "Software developer"
}
```

**Error Response** (401 Unauthorized):
```json
{
  "message": "Authentication required",
  "statusCode": 401,
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/users/me"
}
```

---

### 6. Forgot Password

**Endpoint**: `POST /api/v1/auth/forgot-password`

**Request Body**:
```json
{
  "email": "john@example.com"
}
```

**Success Response** (200 OK):
```json
{
  "message": "Password reset email sent successfully",
  "success": true,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

**Note**: Always return success even if email doesn't exist (security best practice).

---

### 7. Reset Password

**Endpoint**: `POST /api/v1/auth/reset-password`

**Request Body**:
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePass123"
}
```

**Success Response** (200 OK):
```json
{
  "message": "Password reset successfully",
  "success": true,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

**Error Response** (400 Bad Request):
```json
{
  "message": "Invalid or expired reset token",
  "statusCode": 400,
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/auth/reset-password"
}
```

---

### 8. Verify Email

**Endpoint**: `POST /api/v1/auth/verify-email`

**Request Body**:
```json
{
  "token": "verification-token-from-email"
}
```

**Success Response** (200 OK):
```json
{
  "message": "Email verified successfully",
  "success": true,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

**Error Response** (400 Bad Request):
```json
{
  "message": "Invalid or expired verification token",
  "statusCode": 400,
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/auth/verify-email"
}
```

---

### 9. Resend Verification Email

**Endpoint**: `POST /api/v1/auth/resend-verification`

**Request Body**:
```json
{
  "email": "john@example.com"
}
```

**Success Response** (200 OK):
```json
{
  "message": "Verification email sent successfully",
  "success": true,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## JWT Token Requirements

### Access Token

- **Type**: JWT
- **Expiration**: 1 hour (configurable)
- **Payload** must include:
  ```json
  {
    "sub": "user-id",
    "email": "john@example.com",
    "name": "John Doe",
    "role": "USER",
    "exp": 1234567890,
    "iat": 1234567890
  }
  ```

### Refresh Token

- **Type**: JWT or opaque token
- **Expiration**: 7 days (configurable)
- **Storage**: Database (for revocation support)

### Token Format in Response

Frontend expects tokens in the format:
```
Authorization: Bearer <token>
```

## CORS Configuration

### Required Headers

```java
allowedOrigins: http://localhost:5173
allowedMethods: GET, POST, PUT, DELETE, OPTIONS
allowedHeaders: *
allowCredentials: true
```

### Spring Boot Example

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

## Validation Error Format

When validation fails, return errors in this format:

```json
{
  "message": "Validation failed",
  "statusCode": 400,
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/auth/register",
  "validationErrors": [
    {
      "field": "email",
      "message": "Email is required",
      "rejectedValue": ""
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters",
      "rejectedValue": null
    }
  ]
}
```

## HTTP Status Codes

Use appropriate status codes:

| Code | Usage |
|------|-------|
| 200 | Success |
| 201 | Resource created |
| 204 | Success with no content |
| 400 | Bad request / Validation error |
| 401 | Unauthorized / Invalid credentials |
| 403 | Forbidden / Insufficient permissions |
| 404 | Resource not found |
| 409 | Conflict (e.g., email already exists) |
| 422 | Unprocessable entity |
| 429 | Too many requests |
| 500 | Internal server error |
| 503 | Service unavailable |

## Security Requirements

### Password Hashing
- Use BCrypt with strength 10-12
- Never store plain text passwords

### Token Security
- Sign JWT tokens with secure secret
- Validate token signature on every request
- Check token expiration
- Implement token blacklist for logout

### Rate Limiting
Recommended limits:
- Login: 5 attempts per 15 minutes
- Register: 3 attempts per hour
- Password reset: 3 requests per hour

### Input Validation
- Email format validation
- Password strength requirements:
  - Minimum 8 characters
  - At least one uppercase letter
  - At least one lowercase letter
  - At least one number
- Sanitize all inputs

## Error Handling Best Practices

### Generic Errors

Don't expose internal details:
```json
// ❌ Bad
{
  "message": "Database connection failed at line 42 in UserService.java"
}

// ✅ Good
{
  "message": "An error occurred. Please try again later."
}
```

### Authentication Errors

Be vague for security:
```json
// ❌ Bad
{
  "message": "User not found"
}

// ✅ Good
{
  "message": "Invalid email or password"
}
```

## Testing the Integration

### Using cURL

#### Register
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass123"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123"
  }'
```

#### Get Profile
```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access-token>"
```

### Using Frontend

1. Start backend: `./mvnw spring-boot:run`
2. Start frontend: `npm run dev`
3. Test registration flow
4. Test login flow
5. Test protected routes
6. Test token refresh (wait for token to expire)
7. Test logout
8. Test password reset flow

## Implementation Checklist

### Backend Setup
- [ ] Spring Boot project created
- [ ] Spring Security configured
- [ ] JWT library added (e.g., jjwt)
- [ ] Database configured
- [ ] User entity created
- [ ] UserRepository created
- [ ] Email service configured

### Authentication
- [ ] POST /api/v1/auth/register implemented
- [ ] POST /api/v1/auth/login implemented
- [ ] POST /api/v1/auth/refresh implemented
- [ ] POST /api/v1/auth/logout implemented
- [ ] JWT token generation working
- [ ] JWT token validation working
- [ ] Password hashing with BCrypt
- [ ] Token expiration handling

### User Management
- [ ] GET /api/v1/users/me implemented
- [ ] User profile retrieval working
- [ ] User data properly serialized

### Password Management
- [ ] POST /api/v1/auth/forgot-password implemented
- [ ] POST /api/v1/auth/reset-password implemented
- [ ] Password reset token generation
- [ ] Password reset token validation
- [ ] Email sending working

### Email Verification
- [ ] POST /api/v1/auth/verify-email implemented
- [ ] POST /api/v1/auth/resend-verification implemented
- [ ] Verification token generation
- [ ] Verification token validation
- [ ] Email sending working

### Security
- [ ] CORS configured
- [ ] Request validation implemented
- [ ] Rate limiting implemented
- [ ] Security headers configured
- [ ] SQL injection protection
- [ ] XSS protection

### Error Handling
- [ ] Global exception handler
- [ ] Validation error formatting
- [ ] Proper HTTP status codes
- [ ] User-friendly error messages
- [ ] Error logging

### Testing
- [ ] Unit tests for services
- [ ] Integration tests for endpoints
- [ ] Security tests
- [ ] Frontend integration tested
- [ ] All flows tested end-to-end

## Common Issues

### CORS Errors
- Ensure CORS is configured correctly
- Check allowed origins include frontend URL
- Verify OPTIONS requests are handled

### Token Issues
- Check JWT secret is configured
- Verify token expiration times
- Ensure token format is correct

### 401 Errors
- Verify Authorization header format
- Check token is being sent
- Ensure token validation is correct

### Validation Errors
- Check validation error format matches expected structure
- Ensure field names match frontend expectations
- Verify error messages are user-friendly

## Support

For integration issues:
1. Check backend logs
2. Check frontend network tab
3. Verify request/response format
4. Test endpoints with cURL
5. Review error messages

## Resources

- [Spring Security Documentation](https://spring.io/guides/topicals/spring-security-architecture)
- [JWT.io](https://jwt.io/)
- [BCrypt](https://www.baeldung.com/spring-security-registration-password-encoding-bcrypt)
- [Spring Boot REST API Best Practices](https://www.baeldung.com/rest-api-spring-security)
