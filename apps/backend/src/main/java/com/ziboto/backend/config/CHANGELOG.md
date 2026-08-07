# Changelog - Configuration Module

All notable changes to the configuration module are documented here.

## [Unreleased]

### Added
- DotenvConfig for .env file support and environment variable management

---

## [0.2.0] - 2026-08-05

### Added
- CorsConfig for cross-origin request configuration
- WebConfig for web-specific settings
- JpaConfig for JPA and Hibernate configuration
- LoggingConfig for structured logging:
  - Logback configuration
  - File appenders
  - Console appenders
  - Audit log separation
  - Security log separation
  - Log rotation
- OpenApiConfig for Swagger/OpenAPI documentation
- AppProperties configuration class:
  - Security settings
  - CORS settings
  - Storage settings
  - Cache settings
- RedisProperties configuration class:
  - Connection settings
  - Pool configuration
  - Timeout settings
  - Rate limit configuration
  - Failed login tracking configuration
