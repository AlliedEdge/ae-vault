# Changelog - User Management Module

All notable changes to the user management module are documented here.

## [Unreleased]

### Changed
- Enhanced StorageUsageServiceImpl for improved storage tracking
- Updated integration with file management module

---

## [0.2.0] - 2026-08-05

### Added
- UserController with comprehensive user management endpoints:
  - GET /api/v1/users/me - Get current user profile
  - GET /api/v1/users/{id} - Get user by ID (admin)
  - PUT /api/v1/users/profile - Update user profile
  - PATCH /api/v1/users/profile - Partial profile update
  - GET /api/v1/users/sessions - Get active sessions
  - DELETE /api/v1/users/sessions/{sessionId} - Terminate session
  - GET /api/v1/users/storage - Get storage usage information
  - GET /api/v1/users - List users with pagination (admin)
- UserService interface and UserServiceImpl with:
  - User creation and retrieval
  - Profile update (full and partial)
  - User search and listing
  - Session management
  - Storage usage tracking
  - User validation
- User entity with:
  - Basic profile fields (username, email, name, bio)
  - Authentication fields (password, status)
  - Role management (UserRole enum: USER, ADMIN)
  - Storage tracking (used/quota)
  - Last login tracking
  - Timestamps
- UserRepository with custom queries:
  - Find by username, email
  - Existence checks
  - Custom search queries
- User DTOs:
  - UserDto
  - UserResponse
  - UpdateUserRequest
  - UpdateProfileRequest
  - UpdateProfilePatchRequest
  - ProfileUpdateRequest
  - UpdateProfileResponse
  - SessionResponse
  - StorageInfoResponse
  - StorageUsageResponse
- UserMapper for DTO conversions
- UserValidator with validation rules:
  - Username validation (alphanumeric, length)
  - Email validation
  - Password strength validation
  - Name validation
  - Bio length validation
- StorageUsageService and StorageUsageServiceImpl:
  - Storage calculation
  - Quota management
  - Usage percentage tracking
- StorageUsageCacheService with Redis:
  - Cache storage information
  - TTL-based expiration
  - Cache invalidation
- UserStatus enum (ACTIVE, INACTIVE, LOCKED, PENDING_VERIFICATION)
- Database migration V1__Create_users_table.sql

### Security
- Password encryption with BCrypt
- Role-based access control
- Session tracking and management
- Storage quota enforcement
