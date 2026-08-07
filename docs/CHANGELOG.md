# Changelog - Documentation

All notable changes to the project documentation are documented here.

## [Unreleased]

### Removed
- Moved implementation-specific documentation to application folders:
  - ARCHITECTURE.md
  - AUTHENTICATION_SERVICE.md
  - CONNECTION_TEST_GUIDE.md
  - CONNECTION_TEST_RESULTS.md
  - FRONTEND_INTEGRATION_GUIDE.md
  - FRONTEND_INTEGRATION_REQUIREMENTS.md
  - JWT_AUTHENTICATION.md
  - JWT_FLOW_DIAGRAM.md
  - JWT_IMPLEMENTATION_SUMMARY.md
  - LOGIN_FLOW.md
  - LOGIN_FLOW_DIAGRAM.md
  - LOGIN_IMPLEMENTATION_SUMMARY.md
  - QUICK_CONNECTION_TEST.md
  - QUICK_START.md
  - REDIS_INTEGRATION.md
  - REFRESH_TOKEN_IMPLEMENTATION.md
  - REFRESH_TOKEN_QUICK_REFERENCE.md
  - REFRESH_TOKEN_README.md
  - REFRESH_TOKEN_SUMMARY.md
  - REFRESH_TOKEN_TESTING.md
  - SECURITY.md

---

## [0.1.0] - 2026-08-02

### Added
- Low-Level Design (LLD) documentation in docs/architecture/LLD/:
  - 01-authentication-flow.md - Production authentication architecture with HA/DR
  - 02-file-management-system.md - S3 multipart upload design and workflow
  - 03-caching-strategy.md - Redis caching patterns and strategies
  - 04-database-schema.md - Complete database schema with migrations
  - 05-api-specifications.md - REST API endpoints and specifications
  - 06-s3-multipart-upload.md - Detailed S3 multipart upload implementation
  - 07-production-upload-flow.md - Production file upload workflow
  - README.md - LLD module overview and navigation
  - CHANGELOG.md - LLD module change history

---

## [0.0.1] - 2026-08-01

### Added
- High-Level Design (HLD) version 1:
  - architecture/hld-v1.svg - System architecture diagram
- HELP.md - Initial help documentation
