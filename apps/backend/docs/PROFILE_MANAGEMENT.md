# Profile Management Documentation

## Overview
Comprehensive profile management system with proper field restrictions. Users can update their profile information including timezone and language preferences, while sensitive fields are protected from modification.

## Allowed Updates

Users **CAN** update the following fields:
- ✅ **First Name** - 2-100 characters
- ✅ **Last Name** - 2-100 characters
- ✅ **Profile Picture URL (avatarUrl)** - Valid URL, max 500 characters
- ✅ **Timezone** - Valid timezone identifier (e.g., America/New_York, UTC, Europe/London)
- ✅ **Language** - ISO 639-1 language code (e.g., en, en-US, es, fr)

## Restricted Fields

Users **CANNOT** update the following fields:
- ❌ **Email** - Requires separate verification process
- ❌ **Password** - Requires separate password change endpoint
- ❌ **Username** - Permanent, cannot be changed
- ❌ **Roles** - Admin-only modification
- ❌ **Status** - Admin-only modification
- ❌ **Storage Quota** - Admin-only modification
- ❌ **Created Date** - System-managed
- ❌ **Email Verified** - System-managed
- ❌ **Storage Used** - System-managed

## API Endpoints

### 1. Update Profile (PUT)

Complete profile update using PUT semantics.

**Endpoint:** `PUT /api/v1/users/profile`

**Authorization:** Authenticated user (JWT token required)

**Security:** User identity extracted from SecurityContext

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "avatarUrl": "https://example.com/avatar.jpg",
  "timezone": "America/New_York",
  "language": "en-US"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Profile updated successfully",
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
    "timezone": "America/New_York",
    "language": "en-US",
    "storageQuota": 5368709120,
    "storageUsed": 1073741824,
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-08-04T22:00:00"
  },
  "timestamp": "2026-08-04T22:00:00"
}
```

### 2. Update Profile (PATCH)

Partial profile update using PATCH semantics.

**Endpoint:** `PATCH /api/v1/users/profile`

**Authorization:** Authenticated user (JWT token required)

**Security:** User identity extracted from SecurityContext

**Request Body:** (All fields optional)
```json
{
  "firstName": "John",
  "timezone": "Europe/London"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Profile updated successfully",
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
    "timezone": "Europe/London",
    "language": "en-US",
    "storageQuota": 5368709120,
    "storageUsed": 1073741824,
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-08-04T22:05:00"
  },
  "timestamp": "2026-08-04T22:05:00"
}
```

## Field Validation

### First Name / Last Name
- **Min Length:** 2 characters
- **Max Length:** 100 characters
- **Pattern:** Any Unicode characters
- **Optional:** Yes

### Avatar URL
- **Max Length:** 500 characters
- **Pattern:** Valid URL format (http:// or https://)
- **Regex:** `^(https?://)?[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(/.*)?$`
- **Optional:** Yes
- **Examples:**
  - ✅ `https://example.com/avatar.jpg`
  - ✅ `http://cdn.example.com/users/123/avatar.png`
  - ❌ `not-a-url`
  - ❌ `ftp://example.com/file`

### Timezone
- **Max Length:** 50 characters
- **Pattern:** Valid timezone identifier
- **Regex:** `^[A-Za-z/_+-]+$`
- **Validation:** Must be a valid Java ZoneId
- **Optional:** Yes
- **Examples:**
  - ✅ `America/New_York`
  - ✅ `UTC`
  - ✅ `Europe/London`
  - ✅ `Asia/Tokyo`
  - ✅ `US/Pacific`
  - ❌ `Invalid/Timezone`
  - ❌ `EST` (use America/New_York instead)

**Common Timezones:**
```
UTC
America/New_York
America/Chicago
America/Denver
America/Los_Angeles
Europe/London
Europe/Paris
Europe/Berlin
Asia/Tokyo
Asia/Shanghai
Asia/Dubai
Australia/Sydney
```

### Language
- **Min Length:** 2 characters
- **Max Length:** 10 characters
- **Pattern:** ISO 639-1 language code with optional country code
- **Regex:** `^[a-z]{2}(-[A-Z]{2})?$`
- **Validation:** 
  - Language code must be valid ISO 639-1
  - Country code (if provided) must be valid ISO 3166-1 alpha-2
- **Optional:** Yes
- **Examples:**
  - ✅ `en` - English
  - ✅ `en-US` - English (United States)
  - ✅ `en-GB` - English (United Kingdom)
  - ✅ `es` - Spanish
  - ✅ `es-ES` - Spanish (Spain)
  - ✅ `es-MX` - Spanish (Mexico)
  - ✅ `fr` - French
  - ✅ `fr-FR` - French (France)
  - ✅ `de` - German
  - ✅ `ja` - Japanese
  - ✅ `zh` - Chinese
  - ❌ `eng` - Wrong format (use en)
  - ❌ `en-us` - Country code must be uppercase
  - ❌ `EN` - Language code must be lowercase
  - ❌ `en-USA` - Country code must be 2 characters

**Common Language Codes:**
```
en      - English
en-US   - English (United States)
en-GB   - English (United Kingdom)
es      - Spanish
es-ES   - Spanish (Spain)
es-MX   - Spanish (Mexico)
fr      - French
fr-FR   - French (France)
de      - German
de-DE   - German (Germany)
it      - Italian
pt      - Portuguese
pt-BR   - Portuguese (Brazil)
ja      - Japanese
zh      - Chinese
ko      - Korean
ar      - Arabic
ru      - Russian
```

## Validation Rules

### At Least One Field Required
Both PUT and PATCH requests require at least one field to be provided.

**Invalid Request:**
```json
{}
```

**Error Response:** `400 Bad Request`
```json
{
  "success": false,
  "message": "At least one field must be provided for update",
  "timestamp": "2026-08-04T22:00:00"
}
```

### Invalid Timezone
**Invalid Request:**
```json
{
  "timezone": "Invalid/Timezone"
}
```

**Error Response:** `400 Bad Request`
```json
{
  "success": false,
  "message": "Invalid timezone: Invalid/Timezone. Must be a valid timezone identifier (e.g., America/New_York, UTC, Europe/London)",
  "timestamp": "2026-08-04T22:00:00"
}
```

### Invalid Language Code
**Invalid Request:**
```json
{
  "language": "eng"
}
```

**Error Response:** `400 Bad Request`
```json
{
  "success": false,
  "message": "Invalid language code: eng. Must be a valid ISO 639-1 language code (e.g., en, en-US, es, fr)",
  "timestamp": "2026-08-04T22:00:00"
}
```

### Invalid Avatar URL
**Invalid Request:**
```json
{
  "avatarUrl": "not-a-url"
}
```

**Error Response:** `400 Bad Request`
```json
{
  "success": false,
  "message": "Avatar URL must be a valid URL",
  "errors": {
    "avatarUrl": "Avatar URL must be a valid URL"
  },
  "timestamp": "2026-08-04T22:00:00"
}
```

## Security Features

### 1. Field Restriction by DTO Design
Restricted fields are **not included** in the ProfileUpdateRequest DTO, making it impossible to update them through the profile endpoint.

```java
@Data
public class ProfileUpdateRequest {
    private String firstName;      // ✅ Allowed
    private String lastName;       // ✅ Allowed
    private String avatarUrl;      // ✅ Allowed
    private String timezone;       // ✅ Allowed
    private String language;       // ✅ Allowed
    
    // ❌ Not present: email, password, roles, storageQuota, etc.
}
```

### 2. Mapper Field Ignoring
The UserMapper explicitly ignores restricted fields during updates:

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "username", ignore = true)
@Mapping(target = "email", ignore = true)
@Mapping(target = "password", ignore = true)
@Mapping(target = "role", ignore = true)
@Mapping(target = "status", ignore = true)
@Mapping(target = "emailVerified", ignore = true)
@Mapping(target = "storageQuota", ignore = true)
@Mapping(target = "storageUsed", ignore = true)
@Mapping(target = "lastLoginAt", ignore = true)
@Mapping(target = "createdAt", ignore = true)
@Mapping(target = "updatedAt", ignore = true)
void updateUserFromProfileUpdateRequest(ProfileUpdateRequest request, @MappingTarget User user);
```

### 3. User Extraction from SecurityContext
User identity is **always** extracted from the JWT token, never from request parameters:

```java
@PutMapping("/profile")
public ResponseEntity<ApiResponse<UpdateProfileResponse>> updateProfile(
        @Valid @RequestBody ProfileUpdateRequest request) {
    String username = SecurityUtils.getCurrentUsername(); // From JWT
    return userService.updateUserProfile(username, request);
}
```

### 4. Audit Logging
All profile updates are logged for security auditing:

```
Action: UPDATE
Entity: User
Entity ID: 1
Details: User profile updated - Name: John Doe, Timezone: America/New_York, Language: en-US
User: john_doe
Timestamp: 2026-08-04T22:00:00
```

## Implementation Details

### Database Schema Updates

```sql
ALTER TABLE users 
ADD COLUMN timezone VARCHAR(50),
ADD COLUMN language VARCHAR(10);
```

### Entity Fields

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    // ... existing fields
    
    @Column(length = 50)
    private String timezone;
    
    @Column(length = 10)
    private String language;
}
```

### Validation Logic

**Timezone Validation:**
```java
public void validateTimezone(String timezone) {
    try {
        ZoneId.of(timezone); // Java built-in validation
    } catch (Exception e) {
        throw new ValidationException("Invalid timezone");
    }
}
```

**Language Validation:**
```java
public void validateLanguage(String language) {
    String[] parts = language.split("-");
    String languageCode = parts[0].toLowerCase();
    
    List<String> validLanguages = Arrays.asList(Locale.getISOLanguages());
    if (!validLanguages.contains(languageCode)) {
        throw new ValidationException("Invalid language code");
    }
    
    if (parts.length > 1) {
        String countryCode = parts[1].toUpperCase();
        List<String> validCountries = Arrays.asList(Locale.getISOCountries());
        if (!validCountries.contains(countryCode)) {
            throw new ValidationException("Invalid country code");
        }
    }
}
```

## Testing Examples

### cURL Examples

**Update with PUT:**
```bash
curl -X PUT "http://localhost:8080/api/v1/users/profile" \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "avatarUrl": "https://example.com/avatar.jpg",
    "timezone": "America/New_York",
    "language": "en-US"
  }'
```

**Update with PATCH (partial):**
```bash
curl -X PATCH "http://localhost:8080/api/v1/users/profile" \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "timezone": "Europe/London",
    "language": "en-GB"
  }'
```

**Update only timezone:**
```bash
curl -X PATCH "http://localhost:8080/api/v1/users/profile" \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "timezone": "Asia/Tokyo"
  }'
```

## Error Handling

### 400 Bad Request
- No fields provided for update
- Invalid timezone format
- Invalid language code
- Invalid URL format
- Field length violations

### 401 Unauthorized
- Missing JWT token
- Invalid JWT token
- Expired JWT token

### 404 Not Found
- User not found (should not happen with valid token)

## Best Practices

### 1. Use Appropriate HTTP Method
- **PUT** - Complete update (provide all fields)
- **PATCH** - Partial update (provide only changed fields)

### 2. Timezone Storage
Store timezones as IANA timezone identifiers (e.g., "America/New_York") rather than abbreviations (e.g., "EST"):
- ✅ `America/New_York` - Handles DST automatically
- ❌ `EST` - Ambiguous, doesn't handle DST

### 3. Language Code Format
Use ISO 639-1 codes with optional ISO 3166-1 country codes:
- ✅ `en` - Generic English
- ✅ `en-US` - English (United States)
- ✅ `en-GB` - English (United Kingdom)

### 4. Frontend Integration
```javascript
// Update timezone from browser
const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
const response = await fetch('/api/v1/users/profile', {
  method: 'PATCH',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ timezone })
});
```

```javascript
// Update language from browser
const language = navigator.language; // e.g., "en-US"
const response = await fetch('/api/v1/users/profile', {
  method: 'PATCH',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ language })
});
```

## Migration Guide

If updating existing users, provide default values:

```sql
-- Set default timezone for existing users
UPDATE users 
SET timezone = 'UTC' 
WHERE timezone IS NULL;

-- Set default language for existing users
UPDATE users 
SET language = 'en' 
WHERE language IS NULL;
```

## Future Enhancements

1. **Email Change** - Separate endpoint with verification
2. **Password Change** - Secure password update endpoint
3. **Avatar Upload** - Direct file upload instead of URL
4. **Profile Picture Validation** - Validate image dimensions and file size
5. **Timezone Auto-detection** - Detect timezone from IP address
6. **Language Preferences** - Support multiple preferred languages
7. **Profile Completion** - Track profile completion percentage
8. **Profile Privacy** - Control visibility of profile fields
