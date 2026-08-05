# Backend API Integration Guide

## Required Backend Endpoints

The frontend expects the following REST API endpoints to be implemented in your backend:

### Base URL

Configure in `.env`:
```env
VITE_API_URL=http://localhost:3000/api
```

---

## Authentication Endpoints

### 1. Login

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "rememberMe": false
}
```

**Success Response (200):**
```json
{
  "user": {
    "id": "uuid-or-id",
    "email": "user@example.com",
    "name": "John Doe",
    "role": "user",
    "emailVerified": true,
    "createdAt": "2024-01-01T00:00:00Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (401):**
```json
{
  "message": "Invalid credentials",
  "statusCode": 401,
  "error": "Unauthorized"
}
```

---

### 2. Register

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Success Response (201):**
```json
{
  "user": {
    "id": "uuid-or-id",
    "email": "user@example.com",
    "name": "John Doe",
    "role": "user",
    "emailVerified": false,
    "createdAt": "2024-01-01T00:00:00Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (400):**
```json
{
  "message": "Email already exists",
  "statusCode": 400,
  "error": "Bad Request"
}
```

---

### 3. Logout

**Endpoint:** `POST /api/auth/logout`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request Body:** None (or optional refresh token)

**Success Response (200):**
```json
{
  "message": "Logged out successfully"
}
```

**Note:** This endpoint is optional. The frontend will clear tokens locally even if this call fails.

---

### 4. Refresh Token

**Endpoint:** `POST /api/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (401):**
```json
{
  "message": "Invalid or expired refresh token",
  "statusCode": 401,
  "error": "Unauthorized"
}
```

**Important:** This endpoint is called automatically by axios interceptor when access token expires.

---

### 5. Get User Profile

**Endpoint:** `GET /api/auth/profile`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Success Response (200):**
```json
{
  "id": "uuid-or-id",
  "email": "user@example.com",
  "name": "John Doe",
  "role": "user",
  "emailVerified": true,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**Error Response (401):**
```json
{
  "message": "Unauthorized",
  "statusCode": 401,
  "error": "Unauthorized"
}
```

---

### 6. Forgot Password

**Endpoint:** `POST /api/auth/forgot-password`

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Success Response (200):**
```json
{
  "message": "Password reset email sent"
}
```

**Note:** Always return success even if email doesn't exist (security best practice).

---

### 7. Reset Password

**Endpoint:** `POST /api/auth/reset-password`

**Request Body:**
```json
{
  "token": "reset-token-from-email",
  "password": "NewSecurePass123!"
}
```

**Success Response (200):**
```json
{
  "message": "Password reset successfully"
}
```

**Error Response (400):**
```json
{
  "message": "Invalid or expired reset token",
  "statusCode": 400,
  "error": "Bad Request"
}
```

---

### 8. Verify Email

**Endpoint:** `POST /api/auth/verify-email`

**Request Body:**
```json
{
  "token": "verification-token-from-email"
}
```

**Success Response (200):**
```json
{
  "message": "Email verified successfully"
}
```

**Error Response (400):**
```json
{
  "message": "Invalid or expired verification token",
  "statusCode": 400,
  "error": "Bad Request"
}
```

---

### 9. Resend Verification Email

**Endpoint:** `POST /api/auth/resend-verification`

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Success Response (200):**
```json
{
  "message": "Verification email sent"
}
```

---

## JWT Token Structure

### Access Token Payload (Example)

```json
{
  "sub": "user-id",           // Subject (user ID)
  "email": "user@example.com",
  "name": "John Doe",
  "role": "user",
  "iat": 1704067200,          // Issued at (timestamp)
  "exp": 1704070800           // Expires at (timestamp)
}
```

### Refresh Token Payload (Example)

```json
{
  "sub": "user-id",
  "type": "refresh",
  "iat": 1704067200,
  "exp": 1704672000           // Longer expiry (e.g., 7 days)
}
```

### Token Expiry Recommendations

- **Access Token:** 15 minutes
- **Refresh Token:** 7 days
- **Reset Token:** 1 hour
- **Verification Token:** 24 hours

---

## CORS Configuration

Your backend must allow requests from the frontend origin.

### Example (Express.js)

```javascript
const cors = require('cors');

app.use(cors({
  origin: 'http://localhost:5173', // Vite dev server
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
```

### Production

```javascript
app.use(cors({
  origin: process.env.FRONTEND_URL,
  credentials: true
}));
```

---

## Error Handling

### Standard Error Response Format

```json
{
  "message": "Human-readable error message",
  "statusCode": 400,
  "error": "Bad Request",
  "details": {
    "field": "email",
    "reason": "Email format is invalid"
  }
}
```

### HTTP Status Codes

- `200` - Success
- `201` - Created (for register)
- `400` - Bad Request (validation errors)
- `401` - Unauthorized (auth errors)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found
- `500` - Internal Server Error

---

## Security Considerations

### 1. Password Requirements

Enforce these on backend:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- Optional: Special characters

### 2. Rate Limiting

Implement rate limiting on:
- Login endpoint (5 attempts per 15 minutes)
- Register endpoint (3 attempts per hour)
- Password reset (3 requests per hour)

### 3. Token Security

- Use strong secret keys (256-bit minimum)
- Store refresh tokens in database
- Implement token rotation
- Blacklist tokens on logout
- Validate tokens on every request

### 4. Password Storage

- Use bcrypt or argon2 for hashing
- Never store plain text passwords
- Salt rounds: 10-12 for bcrypt

### 5. Email Tokens

- Generate cryptographically secure random tokens
- Store token hash in database
- Set expiration times
- Invalidate after use

---

## Testing Endpoints

### Using cURL

**Login:**
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

**Get Profile:**
```bash
curl -X GET http://localhost:3000/api/auth/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Using Postman

1. Import the collection (create one from endpoints above)
2. Set environment variable for `baseUrl`
3. Save access token to environment after login
4. Use `{{accessToken}}` in Authorization header

---

## Environment Variables (Backend)

```env
# Server
PORT=3000
NODE_ENV=development

# Database
DATABASE_URL=postgresql://user:pass@localhost:5432/ziboto

# JWT
JWT_SECRET=your-256-bit-secret-key
JWT_EXPIRES_IN=15m
REFRESH_TOKEN_SECRET=your-256-bit-refresh-secret
REFRESH_TOKEN_EXPIRES_IN=7d

# Email (for password reset and verification)
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USER=noreply@ziboto.com
SMTP_PASS=your-smtp-password
FROM_EMAIL=noreply@ziboto.com

# Frontend URL (for CORS and email links)
FRONTEND_URL=http://localhost:5173

# Rate Limiting
RATE_LIMIT_WINDOW=15m
RATE_LIMIT_MAX_REQUESTS=100
```

---

## Database Schema Example

### Users Table

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  role VARCHAR(50) DEFAULT 'user',
  email_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

### Refresh Tokens Table

```sql
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
```

### Password Reset Tokens Table

```sql
CREATE TABLE password_reset_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reset_tokens_user ON password_reset_tokens(user_id);
```

### Email Verification Tokens Table

```sql
CREATE TABLE email_verification_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Quick Start Backend Integration

1. **Setup environment variables** in backend
2. **Implement all 9 authentication endpoints**
3. **Configure CORS** to allow frontend origin
4. **Test each endpoint** with curl or Postman
5. **Update frontend `.env`** with correct API URL
6. **Start both frontend and backend**
7. **Test login flow** end-to-end

---

## Troubleshooting

### Issue: CORS errors

**Solution:** Ensure backend CORS configuration includes frontend URL

### Issue: 401 on all requests

**Solution:** Check that Authorization header is being sent with Bearer token

### Issue: Token refresh loop

**Solution:** Ensure refresh endpoint returns new tokens with correct structure

### Issue: Can't login

**Solution:** 
- Check API endpoint URL
- Verify request/response format matches
- Check backend logs for errors
- Test endpoint directly with curl

---

## Next Steps

1. Implement the backend endpoints
2. Test each endpoint individually
3. Connect frontend to backend
4. Test full authentication flow
5. Add email functionality for password reset
6. Implement rate limiting
7. Add logging and monitoring
8. Security audit before production

---

For questions or issues, refer to `AUTH_IMPLEMENTATION.md` for frontend implementation details.
