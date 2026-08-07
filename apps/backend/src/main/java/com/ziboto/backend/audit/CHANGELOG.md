# Changelog - Audit Module

All notable changes to the audit logging module are documented here.

## [Unreleased]

### Changed
- No changes in this release

---

## [0.2.0] - 2026-08-05

### Added
- AuditLog entity for security event tracking:
  - User identification
  - Action tracking (AuditAction enum)
  - Resource type and ID
  - IP address and user agent
  - Success/failure status
  - Error messages
  - Request/response details
  - Timestamps
- AuditAction enum with actions:
  - LOGIN, LOGOUT
  - REGISTER
  - REFRESH_TOKEN
  - PASSWORD_CHANGE, PASSWORD_RESET
  - PROFILE_UPDATE
  - FILE_UPLOAD, FILE_DOWNLOAD, FILE_DELETE
  - UNAUTHORIZED_ACCESS
  - RATE_LIMIT_EXCEEDED
- AuditLogRepository for persistence
- AuditService interface and AuditServiceImpl with:
  - Async audit log creation
  - HTTP request context extraction
  - Comprehensive logging
- Database migration V4__Create_audit_logs_table.sql

### Security
- Comprehensive security event tracking
- IP address and user agent logging
- Async processing to avoid performance impact
