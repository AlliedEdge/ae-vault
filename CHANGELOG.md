# Changelog

All notable changes to the Ziboto project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- File management system with upload, download, and delete capabilities
- Folder management with hierarchical structure support
- Storage service abstraction with local storage implementation
- Database migrations for folders and file metadata tables
- Frontend FileManager page with responsive UI
- DotenvConfig for environment variable management
- PowerShell script for JWT secret generation
- VSCode workspace settings

### Changed
- Updated application.yml configuration for production readiness
- Modified BackendApplication to set default timezone to UTC
- Enhanced storage configuration with configurable file size limits
- Adjusted rate limiting for refresh tokens (increased to 1000 requests per hour)
- Updated FileMetadata and FileMetadataRepository for improved query capabilities
- Refined GlobalExceptionHandler, AuthServiceImpl, and RateLimitService
- Updated StorageUsageServiceImpl for better integration
- Frontend authentication flow improvements
- Package dependencies updated in frontend

### Removed
- Obsolete implementation documentation files from backend and frontend
- Redundant architecture documentation from root docs folder
- Docker compose configuration from backend (moved to infra)
- Historical implementation guides and quick start files

---

## [0.2.0] - 2026-08-05

### Added
- Complete JWT authentication and authorization system
- User registration and login endpoints
- Refresh token mechanism with rotation
- Redis-based caching layer for sessions and rate limiting
- Audit logging system for security events
- User management with profile and storage usage tracking
- Security features: rate limiting, account lockout, token blacklist
- Password strength validation and secure hashing
- Email verification workflow (backend ready)
- Comprehensive exception handling framework
- Frontend authentication pages (Login, Register, Dashboard, etc.)
- Token refresh mechanism with automatic retry
- Protected and guest route components
- Authentication context and Zustand store
- Axios instance with interceptors
- Development and production environment configurations
- Maven build configuration with Spring Boot
- Flyway database migrations
- Redis integration for caching and session management
- Nginx reverse proxy configuration
- Extensive API documentation with OpenAPI/Swagger
- Frontend styling with TailwindCSS
- Animation support with Framer Motion
- Comprehensive testing scripts for authentication flows

### Security
- JWT-based authentication with RS256 signing
- CSRF protection and security headers
- CORS configuration for cross-origin requests
- Rate limiting per endpoint and user
- Failed login attempt tracking
- Account lockout after multiple failed attempts
- Session management with Redis
- Token blacklist for logout
- Password validation rules

---

## [0.1.0] - 2026-08-02

### Added
- Production-ready Low-Level Design (LLD) documentation
- Authentication flow architecture with HA/DR considerations
- File management system design with S3 multipart upload strategy
- Caching strategy documentation
- Database schema design
- API specifications and endpoint documentation
- S3 multipart upload implementation guide
- Production upload flow documentation
- Comprehensive README for LLD module

---

## [0.0.1] - 2026-08-01

### Added
- Initial project structure
- High-Level Design (HLD) version 1
- Project README with architecture overview
- Repository setup with monorepo structure (apps, packages, infra)
- License file (MIT)
- Basic .gitignore configuration
