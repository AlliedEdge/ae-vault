# User Module API Endpoints

## Base URL
```
/api/v1/users
```

## Authentication
All endpoints require JWT Bearer token authentication.

```
Authorization: Bearer <access_token>
```

## Endpoints

### 1. Get Current User Profile

Get the authenticated user's own profile information.

**Endpoint:** `GET /api/v1/users/me`

**Authorization:** Authenticated user

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

---

### 2. Update Current User Profile

Update the authenticated user's own profile.

**Endpoint:** `PUT /api/v1/users/me`

**Authorization:** Authenticated user

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
- `409 Conflict` - Email already exists

---

### 3. Get User by ID

Retrieve user information by ID.

**Endpoint:** `GET /api/v1/users/{userId}`

**Authorization:** Authenticated user (can view own profile) or Admin

**Path Parameters:**
- `userId` (required) - User ID

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "User retrieved successfully",
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
- `404 Not Found` - User not found

---

### 4. Get All Users

Retrieve all users with pagination.

**Endpoint:** `GET /api/v1/users`

**Authorization:** Admin only (`ROLE_ADMIN` or `ROLE_SUPER_ADMIN`)

**Query Parameters:**
- `page` (optional, default: 0) - Page number (0-indexed)
- `size` (optional, default: 20) - Page size
- `sort` (optional) - Sort field and direction (e.g., `username,asc`)

**Example:** `GET /api/v1/users?page=0&size=20&sort=username,asc`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "data": {
    "content": [
      {
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
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false,
    "empty": false
  },
  "timestamp": "2026-08-04T21:00:00"
}
```

**Error Responses:**
- `403 Forbidden` - Not authorized (non-admin user)

---

### 5. Search Users

Search users by username, email, first name, or last name.

**Endpoint:** `GET /api/v1/users/search`

**Authorization:** Admin only (`ROLE_ADMIN` or `ROLE_SUPER_ADMIN`)

**Query Parameters:**
- `query` (required) - Search query string
- `page` (optional, default: 0) - Page number (0-indexed)
- `size` (optional, default: 20) - Page size
- `sort` (optional) - Sort field and direction

**Example:** `GET /api/v1/users/search?query=john&page=0&size=20`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Users found",
  "data": {
    "content": [
      {
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
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 15,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  },
  "timestamp": "2026-08-04T21:00:00"
}
```

**Search Logic:**
- Case-insensitive search
- Matches partial strings in: username, email, firstName, lastName
- Uses SQL LIKE with wildcards

**Error Responses:**
- `403 Forbidden` - Not authorized (non-admin user)

---

### 6. Update User (Admin)

Update any user's information (admin operation).

**Endpoint:** `PUT /api/v1/users/{userId}`

**Authorization:** Admin only (`ROLE_ADMIN` or `ROLE_SUPER_ADMIN`)

**Path Parameters:**
- `userId` (required) - User ID to update

**Request Body:**
```json
{
  "email": "updated@example.com",
  "firstName": "Updated",
  "lastName": "Name",
  "avatarUrl": "https://example.com/updated-avatar.jpg"
}
```

**Notes:**
- All fields are optional
- At least one field must be provided
- Email uniqueness is validated
- Cannot update: username, password, role, status (require separate endpoints)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "User updated successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "updated@example.com",
    "firstName": "Updated",
    "lastName": "Name",
    "role": "ROLE_USER",
    "status": "ACTIVE",
    "emailVerified": true,
    "avatarUrl": "https://example.com/updated-avatar.jpg",
    "storageQuota": 5368709120,
    "storageUsed": 1073741824,
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-08-04T21:10:00"
  },
  "timestamp": "2026-08-04T21:10:00"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid data or no fields provided
- `403 Forbidden` - Not authorized (non-admin user)
- `404 Not Found` - User not found
- `409 Conflict` - Email already exists

---

### 7. Delete User

Soft delete a user (sets status to DELETED).

**Endpoint:** `DELETE /api/v1/users/{userId}`

**Authorization:** Admin only (`ROLE_ADMIN` or `ROLE_SUPER_ADMIN`)

**Path Parameters:**
- `userId` (required) - User ID to delete

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "User deleted successfully",
  "data": null,
  "timestamp": "2026-08-04T21:15:00"
}
```

**Notes:**
- Performs soft delete (sets `status = DELETED`)
- User data is preserved in database
- Deleted users cannot login
- Operation is logged in audit trail

**Error Responses:**
- `403 Forbidden` - Not authorized (non-admin user)
- `404 Not Found` - User not found

---

## Validation Rules

### Email
- Must be valid email format
- Maximum 100 characters
- Must be unique across all users
- Required for registration, optional for updates

### First Name / Last Name
- Minimum 2 characters
- Maximum 100 characters
- Optional for updates

### Avatar URL
- Must start with `http://` or `https://`
- Maximum 500 characters
- Optional

### Username
- 3-50 characters
- Alphanumeric, hyphens, and underscores only
- Must be unique
- Cannot be changed after registration

---

## Common Error Responses

### 400 Bad Request
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

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Unauthorized - Invalid or missing token",
  "timestamp": "2026-08-04T21:00:00"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Forbidden - Admin access required",
  "timestamp": "2026-08-04T21:00:00"
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "User not found with ID: 999",
  "timestamp": "2026-08-04T21:00:00"
}
```

### 409 Conflict
```json
{
  "success": false,
  "message": "Email already exists",
  "timestamp": "2026-08-04T21:00:00"
}
```

---

## Rate Limiting

Rate limiting may apply to prevent abuse. Check the following headers in responses:
- `X-RateLimit-Limit` - Maximum requests per time window
- `X-RateLimit-Remaining` - Remaining requests in current window
- `X-RateLimit-Reset` - Time when rate limit resets

---

## Audit Logging

All mutation operations (UPDATE, DELETE) are automatically logged to the audit trail with:
- User who performed the action
- Entity type and ID
- Action type
- Detailed description of changes
- Timestamp
- IP address and user agent (from request context)

---

## Testing Examples

### cURL Examples

**Get current user profile:**
```bash
curl -X GET "http://localhost:8080/api/v1/users/me" \
  -H "Authorization: Bearer <access_token>"
```

**Update profile:**
```bash
curl -X PUT "http://localhost:8080/api/v1/users/me" \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newemail@example.com",
    "firstName": "John",
    "lastName": "Smith"
  }'
```

**Search users (admin):**
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=john&page=0&size=20" \
  -H "Authorization: Bearer <admin_access_token>"
```

**Delete user (admin):**
```bash
curl -X DELETE "http://localhost:8080/api/v1/users/123" \
  -H "Authorization: Bearer <admin_access_token>"
```
