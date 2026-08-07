# Changelog - Cache Module

All notable changes to the caching module are documented here.

## [Unreleased]

### Changed
- No changes in this release

---

## [0.2.0] - 2026-08-05

### Added
- RedisConfig with comprehensive Redis configuration:
  - Connection factory setup
  - Redis template with JSON serialization
  - Cache manager configuration
  - Connection pooling
  - Timeout configuration
- RedisService for Redis operations:
  - Key-value storage with TTL
  - Hash operations
  - Set operations
  - List operations
  - Expiration management
  - Key deletion
  - Existence checks
- CacheService interface for abstraction
- Cache utilities and helpers
- Comprehensive Redis documentation (package-info.java)

### Configuration
- Configurable Redis connection (host, port, password)
- Connection pooling with HikariCP-style settings
- TTL-based cache expiration
- JSON serialization for complex objects
