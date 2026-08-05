# Authenticated User API Documentation

## Overview
These APIs are designed for authenticated users to manage their own profile, storage, and sessions. User identity is **always extracted from SecurityContext** and **never accepted from the frontend** for security reasons.

## Security Design

### Key Security Principles

1. **No User ID from Frontend**: User identity is extracted from JWT token via `SecurityUtils.getCurrentUsername()`
2. **SecurityContext Only**: All endpoints use Spring Security's `SecurityContextHolder` to get the authenticated user
3. **Authorization Validation**: Session revocation validates that the session belongs to the authenticated user
4. **Audit Logging**: All mutations are logged with user context

### SecurityUtils Class

Location: `com.ziboto.backend.common.util.SecurityUtils`

```java
// Extract current authenticated username
String username = SecurityUtils.getCurrentUsername();

// Get current authentication object
Authentication auth = SecurityUtils.getCurrentAuthentication();

// Check if user has a role
boolean isAdmin = SecurityUtils.hasRole("ROLE_ADMIN");

// Check if authenticated
boolean authenticated = SecurityUtils.isAuthenticated();
```

**Exception Handling:**
- Throws `UnauthorizedException` if no authentication found
- Throws `UnauthorizedException` if unable to extract username

---

## API Endpoints

### Base URL
```
/api/v1/users
```

### Authentication
All endpoints require JWT Bearer token:
```
Authorization: Bearer <access_token>
```

---

## 1. Get Current User Profile

Get the authenticated user's profile information.

**Endpoint:** `GET /api/v1/users/me`

**Authorization:** Authenticated user

**Security:** User extracted from SecurityContext

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ROLE_USER",
    "status": "ACTIVE",
    "emailVerified": true,
    "avatarUrl": "https://example.com/avatar.jpg",
    "storageQuota": 5368709120,
    "storageUsed": 1073741824,
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-08-04T12:00:00"
  },
  "timestamp": "2026-08-04T21:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or missing JWT token
- `404 Not Found` - User not found (should not happen with valid token)

---

## 2. Update Current User Profile (PATCH)

Partially update the authenticated user's profile. Only provided fields will be updated.

**Endpoint:** `PATCH /api/v1/users/profile`

**Authorization:** Authenticated user

**Security:** User extracted from SecurityContext

**HTTP Method:** PATCH (partial update semantics)

**Request Body:**
```json
{
  "email": "newemail@example.com",
  "firstName": "John",
  "lastName": "Smith",
  "avatarUrl": "https://example.com/new-avatar.jpg"
}
```

**Notes:**
- All fields are optional
- At least one field must be provided
- Email uniqueness is validated
- Only provided fields are updated (PATCH semantics)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "newemail@example.com",
    "firstName": "John",
    "lastName": "Smith",
    "role": "ROLE_USER",
    "status": "ACTIVE",
    "emailVerified": true,
    "avatarUrl": "https://example.com/new-avatar.jpg",
    "storageQuota": 5368709120,
    "storageUsed": 1073741824,
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-08-04T21:05:00"
  },
  "timestamp": "2026-08-04T21:05:00"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid data or no fields provided
- `401 Unauthorized` - Invalid or missing JWT token
- `409 Conflict` - Email already exists

**Validation Rules:**
- `email`: Valid email format, max 100 characters, must be unique
- `firstName`: 2-100 characters
- `lastName`: 2-100 characters
- `avatarUrl`: Valid URL (http/https), max 500 characters

---

## 3. Get Storage Information

Get storage quota and usage information for the authenticated user.

**Endpoint:** `GET /api/v1/users/storage`

**Authorization:** Authenticated user

**Security:** User extracted from SecurityContext

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Storage info retrieved successfully",
  "data": {
    "storageQuota": 5368709120,
    "storageUsed": 1073741824,
    "storageAvailable": 4294967296,
    "usagePercentage": 20.0
  },
  "timestamp": "2026-08-04T21:00:00"
}
```

**Response Fields:**
- `storageQuota`: Total storage quota in bytes
- `storageUsed`: Storage currently used in bytes
- `storageAvailable`: Available storage (quota - used) in bytes
- `usagePercentage`: Usage percentage (0.00 - 100.00)

**Error Responses:**
- `401 Unauthorized` - Invalid or missing JWT token
- `404 Not Found` - User not found

---

## 4. Get Active Sessions

Get all active sessions (refresh tokens) for the authenticated user.

**Endpoint:** `GET /api/v1/users/sessions`

**Authorization:** Authenticated user

**Security:** User extracted from SecurityContext

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Sessions retrieved successfully",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "deviceInfo": "Chrome on Windows 10",
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
      "createdAt": "2026-08-01T10:00:00",
      "lastUsedAt": "2026-08-04T20:30:00",
      "expiresAt": "2026-08-31T10:00:00",
      "current": false
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "deviceInfo": "Firefox on macOS",
      "ipAddress": "192.168.1.101",
      "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)...",
      "createdAt": "2026-08-04T15:00:00",
      "lastUsedAt": "2026-08-04T21:00:00",
      "expiresAt": "2026-09-04T15:00:00",
      "current": false
    }
  ],
  "timestamp": "2026-08-04T21:00:00"
}
```

**Session Information:**
- `id`: Session ID (refresh token UUID)
- `deviceInfo`: Device description (from User-Agent parsing)
- `ipAddress`: IP address of the session
- `userAgent`: Full User-Agent string
- `createdAt`: When the session was created
- `lastUsedAt`: Last time the session was used (null if never refreshed)
- `expiresAt`: When the session expires
- `current`: Whether this is the current session (always false in current implementation)

**Notes:**
- Only returns active (non-revoked, non-expired) sessions
- Sessions are ordered by creation time (newest first)
- The current session's refresh token is not stored in headers, so `current` flag is always false

**Error Responses:**
- `401 Unauthorized` - Invalid or missing JWT token
- `404 Not Found` - User not found

---

## 5. Revoke Session

Revoke a specific session (logout from a device).

**Endpoint:** `DELETE /api/v1/users/sessions/{sessionId}`

**Authorization:** Authenticated user

**Security:** 
- User extracted from SecurityContext
- Validates session belongs to authenticated user
- Throws `403 Forbidden` if session belongs to different user

**Path Parameters:**
- `sessionId` (required) - Session ID (UUID format)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Session revoked successfully",
  "data": null,
  "timestamp": "2026-08-04T21:15:00"
}
```

**Security Validation:**
1. Validates `sessionId` is valid UUID format
2. Fetches the session from database
3. Verifies the session belongs to the authenticated user
4. Only then revokes the session

**Error Responses:**
- `400 Bad Request` - Invalid session ID format
- `401 Unauthorized` - Invalid or missing JWT token
- `403 Forbidden` - Session does not belong to current user
- `404 Not Found` - Session not found

**Audit Logging:**
- Action: `DELETE`
- Entity: `Session`
- Details: Includes session ID and IP address

**Use Cases:**
- User wants to logout from a specific device
- User sees suspicious session and wants to revoke it
- Security measure after password change (revoke all other sessions)

---

## Implementation Details

### Service Layer

**Location:** `com.ziboto.backend.user.service.UserServiceImpl`

**Key Methods:**
```java
// Get current user profile
UserResponse getCurrentUser(String username)

// Patch profile with partial updates
UpdateProfileResponse patchCurrentUserProfile(String username, UpdateProfilePatchRequest request)

// Get storage information
StorageInfoResponse getUserStorageInfo(String username)

// Get all active sessions
List<SessionResponse> getUserSessions(String username)

// Revoke a specific session
void revokeSession(String username, String sessionId)
```

**Validation:**
- Uses `UserValidator` for business rule validation
- Email uniqueness checked before updates
- Session ownership validated before revocation
- At least one field required for profile updates

**Transaction Management:**
- Read operations: `@Transactional(readOnly = true)`
- Write operations: `@Transactional`

### DTOs

**UpdateProfilePatchRequest:**
```java
{
  String email;        // Optional, validated
  String firstName;    // Optional, 2-100 chars
  String lastName;     // Optional, 2-100 chars
  String avatarUrl;    // Optional, valid URL
}
```

**StorageInfoResponse:**
```java
{
  Long storageQuota;      // Total quota in bytes
  Long storageUsed;       // Used storage in bytes
  Long storageAvailable;  // Available storage in bytes
  Double usagePercentage; // Usage percentage (0-100)
}
```

**SessionResponse:**
```java
{
  String id;                  // Session UUID
  String deviceInfo;          // Device description
  String ipAddress;           // IP address
  String userAgent;           // User agent string
  LocalDateTime createdAt;    // Creation time
  LocalDateTime lastUsedAt;   // Last used time
  LocalDateTime expiresAt;    // Expiration time
  Boolean current;            // Is current session
}
```

---

## Testing Examples

### cURL Examples

**Get current user profile:**
```bash
curl -X GET "http://localhost:8080/api/v1/users/me" \
  -H "Authorization: Bearer <access_token>"
```

**Update profile (PATCH):**
```bash
curl -X PATCH "http://localhost:8080/api/v1/users/profile" \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newemail@example.com",
    "firstName": "John"
  }'
```

**Get storage info:**
```bash
curl -X GET "http://localhost:8080/api/v1/users/storage" \
  -H "Authorization: Bearer <access_token>"
```

**Get sessions:**
```bash
curl -X GET "http://localhost:8080/api/v1/users/sessions" \
  -H "Authorization: Bearer <access_token>"
```

**Revoke session:**
```bash
curl -X DELETE "http://localhost:8080/api/v1/users/sessions/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <access_token>"
```

---

## Security Best Practices

### 1. Never Accept User ID from Frontend
❌ **WRONG:**
```java
@GetMapping("/profile/{userId}")
public ResponseEntity<UserResponse> getProfile(@PathVariable Long userId) {
    return userService.getUserById(userId); // SECURITY RISK!
}
```

✅ **CORRECT:**
```java
@GetMapping("/me")
public ResponseEntity<UserResponse> getProfile() {
    String username = SecurityUtils.getCurrentUsername(); // From JWT token
    return userService.getCurrentUser(username);
}
```

### 2. Validate Ownership
Always validate that resources belong to the authenticated user:

```java
public void revokeSession(String username, String sessionId) {
    RefreshToken token = refreshTokenRepository.findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    
    // Validate ownership
    if (!token.getUser().getUsername().equals(username)) {
        throw new UnauthorizedException("Not authorized");
    }
    
    // Then perform action
    token.setRevoked(true);
    refreshTokenRepository.save(token);
}
```

### 3. Use PATCH for Partial Updates
PATCH semantics allow clients to update only specific fields:

```java
// Client only wants to update email
PATCH /api/v1/users/profile
{
  "email": "new@example.com"
}

// Other fields (firstName, lastName, avatarUrl) remain unchanged
```

### 4. Proper HTTP Status Codes
- `200 OK` - Successful operation
- `400 Bad Request` - Invalid input data
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Authenticated but not authorized for this resource
- `404 Not Found` - Resource doesn't exist
- `409 Conflict` - Data conflict (e.g., duplicate email)

---

## Error Handling

### Common Error Responses

**Unauthorized (401):**
```json
{
  "success": false,
  "message": "User is not authenticated",
  "timestamp": "2026-08-04T21:00:00"
}
```

**Forbidden (403):**
```json
{
  "success": false,
  "message": "You are not authorized to revoke this session",
  "timestamp": "2026-08-04T21:00:00"
}
```

**Not Found (404):**
```json
{
  "success": false,
  "message": "Session not found",
  "timestamp": "2026-08-04T21:00:00"
}
```

**Validation Error (400):**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email must be valid",
    "firstName": "First name must be between 2 and 100 characters"
  },
  "timestamp": "2026-08-04T21:00:00"
}
```

**Conflict (409):**
```json
{
  "success": false,
  "message": "Email already exists",
  "timestamp": "2026-08-04T21:00:00"
}
```

---

## Audit Trail

All mutations are automatically logged to the audit trail:

**Profile Update:**
```
User: john_doe
Action: UPDATE
Entity: User
Entity ID: 1
Details: User profile patched - Email: old@example.com -> new@example.com
Timestamp: 2026-08-04T21:05:00
```

**Session Revocation:**
```
User: john_doe
Action: DELETE
Entity: Session
Entity ID: 1
Details: Session revoked: 550e8400-e29b-41d4-a716-446655440000 from 192.168.1.100
Timestamp: 2026-08-04T21:15:00
```

---

## Future Enhancements

1. **Current Session Detection**: Identify which session is currently being used
2. **Session Naming**: Allow users to name their devices/sessions
3. **Bulk Session Revocation**: Revoke all sessions except current
4. **Session Activity**: Show detailed activity log per session
5. **Storage Breakdown**: Show storage usage by file type/bucket
6. **Profile Picture Upload**: Direct upload of avatar images
7. **Email Verification**: Re-verify email after change
8. **Password Change**: Add endpoint to change password
