# Low-Level Design: API Specifications

## Overview
Complete REST API specification for Ziboto v1, including endpoints, request/response formats, authentication, and error handling.

## API Structure

### Base URL
```
Production:  https://api.ziboto.com/v1
Development: http://localhost:8080/api/v1
```

### Versioning
- Version included in URL path: `/api/v1/`
- Future versions: `/api/v2/`, `/api/v3/`, etc.

### Content Type
```
Content-Type: application/json
Accept: application/json
```

## Authentication

### Bearer Token Authentication
```http
Authorization: Bearer {jwt-access-token}
```

### Token Refresh
When access token expires (15 min), use refresh token to get new tokens.

## API Endpoints

## 1. Authentication Endpoints

### 1.1 User Registration

**Endpoint**: `POST /auth/register`  
**Authentication**: None  
**Rate Limit**: 5 requests per hour per IP

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Validation Rules**:
- Email: Valid format, max 255 chars
- Password: Min 8 chars, must contain uppercase, lowercase, number, special char
- First/Last Name: 2-100 chars, letters only

**Success Response** (201 Created):
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isEmailVerified": false,
    "createdAt": "2026-08-02T10:30:00Z"
  }
}
```

**Error Responses**:
```json
// 400 Bad Request - Validation Error
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": {
      "email": "Email already exists",
      "password": "Password must contain at least one uppercase letter"
    }
  }
}

// 429 Too Many Requests
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many registration attempts. Please try again later.",
    "retryAfter": 3600
  }
}
```

---

### 1.2 User Login

**Endpoint**: `POST /auth/login`  
**Authentication**: None  
**Rate Limit**: 10 requests per minute per IP

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "storageQuota": 5368709120,
      "storageUsed": 1234567890
    }
  }
}
```

**Error Responses**:
```json
// 401 Unauthorized - Invalid Credentials
{
  "success": false,
  "error": {
    "code": "AUTH_001",
    "message": "Invalid email or password"
  }
}

// 403 Forbidden - Account Disabled
{
  "success": false,
  "error": {
    "code": "AUTH_006",
    "message": "Your account has been disabled. Please contact support."
  }
}
```

---

### 1.3 Refresh Token

**Endpoint**: `POST /auth/refresh`  
**Authentication**: None  
**Rate Limit**: 20 requests per minute

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

---

### 1.4 Logout

**Endpoint**: `POST /auth/logout`  
**Authentication**: Required  
**Rate Limit**: 10 requests per minute

**Request Body**: None

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

## 2. File Management Endpoints

### 2.1 Upload File

**Endpoint**: `POST /files/upload`  
**Authentication**: Required  
**Content-Type**: `multipart/form-data`  
**Rate Limit**: 100 requests per hour per user  
**Note**: For files >100MB, use multipart upload endpoints (see section 2.8-2.12)

**Request** (multipart/form-data):
```
file: [binary data]
folderId: "550e8400-e29b-41d4-a716-446655440000" (optional)
```

**Success Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "fileId": "660e8400-e29b-41d4-a716-446655440000",
    "fileName": "document.pdf",
    "fileSize": 1048576,
    "mimeType": "application/pdf",
    "sha256Hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "folderId": "550e8400-e29b-41d4-a716-446655440000",
    "folderPath": "/Documents/Projects",
    "uploadedAt": "2026-08-02T10:30:00Z",
    "downloadUrl": "/api/v1/files/660e8400-e29b-41d4-a716-446655440000/download"
  }
}
```

**Error Responses**:
```json
// 413 Payload Too Large
{
  "success": false,
  "error": {
    "code": "FILE_002",
    "message": "File size exceeds maximum allowed size of 500MB",
    "maxSize": 524288000
  }
}

// 507 Insufficient Storage
{
  "success": false,
  "error": {
    "code": "FILE_003",
    "message": "Storage quota exceeded",
    "available": 104857600,
    "required": 209715200
  }
}

// 400 Bad Request - Invalid File Type
{
  "success": false,
  "error": {
    "code": "FILE_001",
    "message": "File type not allowed",
    "allowedTypes": ["image/jpeg", "image/png", "application/pdf"]
  }
}
```

---

### 2.2 Download File

**Endpoint**: `GET /files/{fileId}/download`  
**Authentication**: Required  
**Rate Limit**: 200 requests per hour per user

**Path Parameters**:
- `fileId`: UUID of the file

**Success Response** (200 OK):
- Returns file stream
- Headers:
  ```
  Content-Type: {file-mime-type}
  Content-Disposition: attachment; filename="{filename}"
  Content-Length: {file-size}
  ```

**Error Responses**:
```json
// 404 Not Found
{
  "success": false,
  "error": {
    "code": "FILE_004",
    "message": "File not found"
  }
}

// 403 Forbidden
{
  "success": false,
  "error": {
    "code": "FILE_005",
    "message": "You don't have permission to download this file"
  }
}
```

---

### 2.3 Get File Metadata

**Endpoint**: `GET /files/{fileId}`  
**Authentication**: Required  
**Rate Limit**: 200 requests per minute per user

**Path Parameters**:
- `fileId`: UUID of the file

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "fileId": "660e8400-e29b-41d4-a716-446655440000",
    "fileName": "document.pdf",
    "fileSize": 1048576,
    "mimeType": "application/pdf",
    "sha256Hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "folderId": "550e8400-e29b-41d4-a716-446655440000",
    "folderPath": "/Documents/Projects",
    "description": "Project requirements document",
    "tags": ["project", "requirements", "2026"],
    "owner": {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "name": "John Doe"
    },
    "downloadCount": 5,
    "uploadedAt": "2026-08-02T10:30:00Z",
    "lastModified": "2026-08-02T10:30:00Z",
    "lastDownloaded": "2026-08-02T12:00:00Z"
  }
}
```

---

### 2.4 List Files

**Endpoint**: `GET /files`  
**Authentication**: Required  
**Rate Limit**: 100 requests per minute per user

**Query Parameters**:
```
folderId: UUID (optional, defaults to root)
page: integer (default: 0)
size: integer (default: 20, max: 100)
sort: string (default: "uploadedAt,desc")
  Options: 
    - uploadedAt,desc
    - uploadedAt,asc
    - fileName,asc
    - fileName,desc
    - fileSize,asc
    - fileSize,desc
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "files": [
      {
        "fileId": "660e8400-e29b-41d4-a716-446655440000",
        "fileName": "document.pdf",
        "fileSize": 1048576,
        "mimeType": "application/pdf",
        "folderId": "550e8400-e29b-41d4-a716-446655440000",
        "uploadedAt": "2026-08-02T10:30:00Z",
        "downloadCount": 5
      }
    ],
    "pagination": {
      "currentPage": 0,
      "totalPages": 5,
      "totalElements": 98,
      "pageSize": 20,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

---

### 2.5 Update File Metadata

**Endpoint**: `PATCH /files/{fileId}`  
**Authentication**: Required  
**Rate Limit**: 100 requests per hour per user

**Path Parameters**:
- `fileId`: UUID of the file

**Request Body**:
```json
{
  "fileName": "updated-document.pdf",
  "description": "Updated project requirements",
  "tags": ["project", "requirements", "2026", "updated"]
}
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "File metadata updated successfully",
  "data": {
    "fileId": "660e8400-e29b-41d4-a716-446655440000",
    "fileName": "updated-document.pdf",
    "description": "Updated project requirements",
    "tags": ["project", "requirements", "2026", "updated"],
    "updatedAt": "2026-08-02T11:00:00Z"
  }
}
```

---

### 2.6 Delete File

**Endpoint**: `DELETE /files/{fileId}`  
**Authentication**: Required  
**Rate Limit**: 100 requests per hour per user

**Path Parameters**:
- `fileId`: UUID of the file

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "File deleted successfully"
}
```

---

### 2.7 Search Files

**Endpoint**: `GET /files/search`  
**Authentication**: Required  
**Rate Limit**: 50 requests per minute per user

**Query Parameters**:
```
q: string (search query, required)
mimeType: string (optional filter)
minSize: integer (bytes, optional)
maxSize: integer (bytes, optional)
fromDate: ISO 8601 date (optional)
toDate: ISO 8601 date (optional)
page: integer (default: 0)
size: integer (default: 20, max: 100)
```

**Example**:
```
GET /files/search?q=project&mimeType=application/pdf&page=0&size=20
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "results": [
      {
        "fileId": "660e8400-e29b-41d4-a716-446655440000",
        "fileName": "project-requirements.pdf",
        "fileSize": 1048576,
        "mimeType": "application/pdf",
        "folderPath": "/Documents/Projects",
        "uploadedAt": "2026-08-02T10:30:00Z",
        "relevanceScore": 0.95
      }
    ],
    "pagination": {
      "currentPage": 0,
      "totalPages": 2,
      "totalElements": 35,
      "pageSize": 20
    },
    "searchTime": 45
  }
}
```

---

### 2.8 Initiate Multipart Upload

**Endpoint**: `POST /files/multipart/initiate`  
**Authentication**: Required  
**Rate Limit**: 50 requests per hour per user  
**Use Case**: For files >100MB

**Request Body**:
```json
{
  "fileName": "large-video.mp4",
  "fileSize": 524288000,
  "mimeType": "video/mp4",
  "folderId": "550e8400-e29b-41d4-a716-446655440000",
  "sha256Hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

**Success Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "uploadId": "upload-session-uuid",
    "s3UploadId": "AWS-multipart-upload-id",
    "fileId": "660e8400-e29b-41d4-a716-446655440000",
    "partSize": 10485760,
    "totalParts": 50,
    "expiresAt": "2026-08-03T10:30:00Z"
  }
}
```

---

### 2.9 Upload Part

**Endpoint**: `PUT /files/multipart/upload/{uploadId}/part/{partNumber}`  
**Authentication**: Required  
**Content-Type**: `application/octet-stream`  
**Rate Limit**: 1000 requests per hour per user

**Path Parameters**:
- `uploadId`: Upload session ID from initiate response
- `partNumber`: Part number (1-based, sequential)

**Request Body**: Binary data (chunk of file, typically 10MB)

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "partNumber": 1,
    "eTag": "\"3858f62230ac3c915f300c664312c63f\"",
    "uploadedBytes": 10485760,
    "totalBytes": 524288000,
    "progress": 2.0
  }
}
```

---

### 2.10 Complete Multipart Upload

**Endpoint**: `POST /files/multipart/complete/{uploadId}`  
**Authentication**: Required  
**Rate Limit**: 50 requests per hour per user

**Path Parameters**:
- `uploadId`: Upload session ID

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "fileId": "660e8400-e29b-41d4-a716-446655440000",
    "fileName": "large-video.mp4",
    "fileSize": 524288000,
    "mimeType": "video/mp4",
    "s3Key": "users/{userId}/files/{fileId}/large-video.mp4",
    "uploadedAt": "2026-08-02T10:45:00Z"
  }
}
```

**Error Responses**:
```json
// 400 Bad Request - Not All Parts Uploaded
{
  "success": false,
  "error": {
    "code": "INCOMPLETE_UPLOAD",
    "message": "Not all parts have been uploaded",
    "uploadedParts": 45,
    "totalParts": 50
  }
}
```

---

### 2.11 Abort Multipart Upload

**Endpoint**: `DELETE /files/multipart/abort/{uploadId}`  
**Authentication**: Required  
**Rate Limit**: 50 requests per hour per user

**Path Parameters**:
- `uploadId`: Upload session ID

**Success Response** (204 No Content)

---

### 2.12 Get Upload Status

**Endpoint**: `GET /files/multipart/status/{uploadId}`  
**Authentication**: Required  
**Rate Limit**: 200 requests per minute per user

**Path Parameters**:
- `uploadId`: Upload session ID

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "uploadId": "upload-session-uuid",
    "fileId": "660e8400-e29b-41d4-a716-446655440000",
    "fileName": "large-video.mp4",
    "fileSize": 524288000,
    "totalParts": 50,
    "uploadedParts": 35,
    "progress": 70.0,
    "status": "IN_PROGRESS",
    "expiresAt": "2026-08-03T10:30:00Z",
    "missingParts": [36, 37, 38, 39, 40]
  }
}
```

---

## 3. Folder Management Endpoints

### 3.1 Create Folder

**Endpoint**: `POST /folders`  
**Authentication**: Required  
**Rate Limit**: 100 requests per hour per user

**Request Body**:
```json
{
  "folderName": "Projects",
  "parentFolderId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Success Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "folderId": "770e8400-e29b-41d4-a716-446655440000",
    "folderName": "Projects",
    "parentFolderId": "550e8400-e29b-41d4-a716-446655440000",
    "folderPath": "/Documents/Projects",
    "createdAt": "2026-08-02T10:30:00Z"
  }
}
```

---

### 3.2 List Folders

**Endpoint**: `GET /folders`  
**Authentication**: Required  
**Rate Limit**: 100 requests per minute per user

**Query Parameters**:
```
parentFolderId: UUID (optional, defaults to root)
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "currentFolder": {
      "folderId": "550e8400-e29b-41d4-a716-446655440000",
      "folderName": "Documents",
      "folderPath": "/Documents"
    },
    "subfolders": [
      {
        "folderId": "770e8400-e29b-41d4-a716-446655440000",
        "folderName": "Projects",
        "folderPath": "/Documents/Projects",
        "fileCount": 12,
        "createdAt": "2026-08-02T10:30:00Z"
      }
    ],
    "breadcrumb": [
      {"folderId": null, "folderName": "Root", "folderPath": "/"},
      {"folderId": "550e8400-e29b-41d4-a716-446655440000", "folderName": "Documents", "folderPath": "/Documents"}
    ]
  }
}
```

---

### 3.3 Delete Folder

**Endpoint**: `DELETE /folders/{folderId}`  
**Authentication**: Required  
**Rate Limit**: 50 requests per hour per user

**Path Parameters**:
- `folderId`: UUID of the folder

**Query Parameters**:
```
recursive: boolean (default: false)
  - false: Delete only if empty
  - true: Delete folder and all contents
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "Folder deleted successfully",
  "deletedItems": {
    "folders": 3,
    "files": 15,
    "freedSpace": 52428800
  }
}
```

---

## 4. User Management Endpoints

### 4.1 Get User Profile

**Endpoint**: `GET /users/me`  
**Authentication**: Required  
**Rate Limit**: 100 requests per minute per user

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isEmailVerified": true,
    "storage": {
      "quotaBytes": 5368709120,
      "usedBytes": 1234567890,
      "usagePercentage": 23.0,
      "fileCount": 42
    },
    "createdAt": "2026-01-15T10:30:00Z",
    "lastLoginAt": "2026-08-02T09:00:00Z"
  }
}
```

---

### 4.2 Update User Profile

**Endpoint**: `PATCH /users/me`  
**Authentication**: Required  
**Rate Limit**: 20 requests per hour per user

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Smith"
}
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "John",
    "lastName": "Smith",
    "updatedAt": "2026-08-02T11:00:00Z"
  }
}
```

---

### 4.3 Change Password

**Endpoint**: `POST /users/me/change-password`  
**Authentication**: Required  
**Rate Limit**: 5 requests per hour per user

**Request Body**:
```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewSecurePass456!"
}
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "Password changed successfully"
}
```

---

## Error Response Format

All error responses follow this standard format:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {} // Optional additional information
  },
  "timestamp": "2026-08-02T10:30:00Z",
  "path": "/api/v1/files/upload"
}
```

## HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful GET, PATCH, DELETE |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful DELETE (no response body) |
| 400 | Bad Request | Validation error, malformed request |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Authenticated but not authorized |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists |
| 413 | Payload Too Large | File size exceeds limit |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Server temporarily unavailable |
| 507 | Insufficient Storage | Storage quota exceeded |

---

**Version**: 1.0  
**Last Updated**: 2026-08-02  
**Author**: Ziboto Team
