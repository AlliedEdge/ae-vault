# Changelog - Exception Handling Module

All notable changes to the exception handling module are documented here.

## [Unreleased]

### Changed
- Enhanced GlobalExceptionHandler with improved error responses
- Better error message formatting

---

## [0.2.0] - 2026-08-05

### Added
- GlobalExceptionHandler with comprehensive exception handling:
  - @RestControllerAdvice configuration
  - HTTP status code mapping
  - Structured error responses
  - Validation error handling
  - Authentication error handling
  - Authorization error handling
  - Resource not found handling
  - Conflict handling
  - Rate limit handling
  - Generic exception fallback
  - Logging of all exceptions
- Custom exception classes:
  - BaseException (abstract base)
  - ResourceNotFoundException
  - UnauthorizedException
  - AccountLockedException
  - InvalidTokenException
  - ValidationException
  - ConflictException
  - RateLimitExceededException
- ErrorCode constants for standardized error codes
- ValidationMessages constants for validation error messages
