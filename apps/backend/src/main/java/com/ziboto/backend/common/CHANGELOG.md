# Changelog - Common Module

All notable changes to the common utilities module are documented here.

## [Unreleased]

### Changed
- No changes in this release

---

## [0.2.0] - 2026-08-05

### Added
- BaseEntity abstract class with audit fields:
  - createdAt timestamp
  - updatedAt timestamp
  - JPA annotations for automatic timestamping
- ApiResponse generic wrapper for consistent API responses:
  - Success/error flag
  - Message field
  - Data field
  - Timestamp
  - Static factory methods
- PageResponse for paginated results:
  - Content list
  - Page metadata (number, size, total)
- ErrorCode constants:
  - Authentication errors
  - Authorization errors
  - Validation errors
  - Resource errors
  - System errors
- ValidationMessages constants:
  - Field validation messages
  - Format validation messages
  - Business rule messages
- SecurityUtils utility class:
  - Current user retrieval
  - Authentication checks
  - Role validation
