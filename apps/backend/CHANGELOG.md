# Changelog - Backend

All notable changes to the Ziboto Backend application are documented here.

## [Unreleased]

### Added
- File management REST API with FileController
- Folder management REST API with FolderController
- FileService for file operations (upload, download, delete, list, search)
- FolderService for folder operations (create, rename, move, delete)
- StorageService abstraction layer
- LocalStorageService implementation for file system storage
- File and folder DTOs (FileMetadataResponse, FileUploadResponse, FolderRequest, FolderResponse)
- Folder entity with hierarchical structure support
- FolderRepository for database operations
- Database migrations: V8__Create_folders_table.sql and V9__Create_file_metadata_table.sql
- DotenvConfig for .env file support
- PowerShell version of JWT secret generation script
- META-INF resources directory
- Static resources directory

### Changed
- Updated pom.xml dependencies (version updates and optimizations)
- Modified application.yml for production readiness:
  - Changed JPA DDL auto mode from validate to update
  - Added serverTimezone property for database connections
  - Set default JWT secret with fallback
  - Updated storage configuration to use local storage for Version 1
  - Reduced local storage base path to ./storage
  - Added file size limits (500MB default)
  - Increased refresh token rate limit to 1000 requests/hour
- Updated BackendApplication to set default timezone to UTC
- Enhanced FileMetadata entity with improved field mapping
- Updated FileMetadataRepository with additional query methods
- Refined GlobalExceptionHandler for better error responses
- Improved AuthServiceImpl for enhanced authentication logic
- Updated RateLimitService for better rate limiting
- Enhanced StorageUsageServiceImpl for accurate storage tracking

### Removed
- AUTHENTICATED_USER_API_IMPLEMENTATION.md
- HOW_TO_RUN.md
- JWT_FIX_SUMMARY.md
- PROFILE_MANAGEMENT_IMPLEMENTATION.md
- QUICK_FIX.md
- REDIS_IMPLEMENTATION_COMPLETE.md
- REDIS_VERIFICATION_CHECKLIST.md
- SECURITY_IMPLEMENTATION_SUMMARY.md
- START_HERE.txt
- STORAGE_USAGE_IMPLEMENTATION.md
- USER_MODULE_IMPLEMENTATION.md
- docker-compose.yml (moved to infra)
- docs/ directory (implementation guides removed, moved to centralized docs)

---

## [0.2.0] - 2026-08-05

### Added
- Complete Spring Boot application structure
- JWT authentication and authorization system
- User registration and login REST APIs
- Refresh token mechanism with rotation
- Redis integration for caching and session management
- Audit logging system (AuditLog entity, repository, and service)
- User management module:
  - User entity with roles and status
  - UserController with profile and storage endpoints
  - UserService and UserServiceImpl
  - UserRepository with custom queries
  - User DTOs and mappers
- Authentication module:
  - AuthController with login, register, refresh, logout, verify endpoints
  - AuthService and AuthServiceImpl with comprehensive security features
  - RefreshToken entity and repository
  - CustomUserDetailsService for Spring Security integration
  - Token blacklist service
  - Session cache service
  - OTP cache service
  - Failed login attempt tracking
  - Rate limiting service
  - Registration service
- File storage module (initial):
  - FileMetadata entity
  - FileMetadataRepository
  - FileStorageService and LocalFileStorageService
- Security configuration:
  - SecurityConfig with JWT filter chain
  - JwtTokenProvider for token generation and validation
  - JwtAuthenticationFilter for request interception
  - JwtAuthenticationEntryPoint for unauthorized access handling
  - SecurityHeadersFilter for HTTP security headers
  - JwtProperties configuration
- Common utilities:
  - BaseEntity for auditing fields
  - ApiResponse and PageResponse DTOs
  - ErrorCode constants
  - ValidationMessages constants
  - SecurityUtils
- Exception handling:
  - GlobalExceptionHandler
  - Custom exception classes (ResourceNotFoundException, UnauthorizedException, etc.)
- Configuration:
  - CorsConfig for cross-origin requests
  - WebConfig for web-specific settings
  - JpaConfig for JPA configuration
  - LoggingConfig for structured logging
  - OpenApiConfig for Swagger documentation
  - AppProperties and RedisProperties
  - RedisConfig and RedisService
- Flyway database migrations:
  - V1__Create_users_table.sql
  - V2__Create_buckets_table.sql
  - V3__Create_file_metadata_table.sql
  - V4__Create_audit_logs_table.sql
  - V5__Add_last_login_at_to_users.sql
  - V6__Create_refresh_tokens_table.sql
- Maven configuration (pom.xml) with all dependencies
- Application configuration (application.yml, application-dev.yml, application-prod.yml)
- Shell scripts for development (run-dev.sh, run-with-env.sh, RUN_ME.sh)
- JWT secret generation script
- Nginx reverse proxy configuration
- Testing utilities and scripts
- Unit tests (BackendApplicationTests, RegistrationServiceImplTest)
- .env.example for environment variables
- Maven wrapper (mvnw, mvnw.cmd)
- README with setup and usage instructions

### Security
- JWT authentication with configurable expiration
- Password encryption with BCrypt
- CSRF protection
- CORS configuration
- Rate limiting per endpoint
- Failed login attempt tracking
- Account lockout mechanism
- Token blacklist for logout
- Session management with Redis
- Security headers (X-Frame-Options, CSP, etc.)

---

## [0.0.1] - 2026-08-01

### Added
- Initial backend project setup
- Basic project structure placeholder
