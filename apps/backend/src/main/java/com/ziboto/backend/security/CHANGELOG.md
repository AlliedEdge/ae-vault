# Changelog - Security Module

All notable changes to the security module are documented here.

## [Unreleased]

### Changed
- Minor refinements to security configuration

---

## [0.2.0] - 2026-08-05

### Added
- SecurityConfig with comprehensive Spring Security configuration:
  - JWT-based authentication
  - Stateless session management
  - Public and protected endpoint configuration
  - CORS integration
  - CSRF protection
  - Security headers
- JwtTokenProvider for JWT operations:
  - Token generation (access and refresh)
  - Token validation and parsing
  - Claims extraction
  - User authentication from token
  - Configurable expiration
- JwtAuthenticationFilter for request interception:
  - Authorization header extraction
  - Token validation
  - Security context setup
  - Error handling
- JwtAuthenticationEntryPoint for unauthorized access:
  - Custom error responses
  - Logging of authentication failures
- JwtAuthenticationSuccessHandler for login success:
  - Token generation on successful authentication
  - Custom response formatting
- SecurityHeadersFilter for HTTP security headers:
  - X-Frame-Options
  - X-Content-Type-Options
  - X-XSS-Protection
  - Content-Security-Policy
  - Strict-Transport-Security
  - Referrer-Policy
- JwtProperties for JWT configuration:
  - Secret key
  - Token expiration times
  - Issuer configuration
- SecurityUtils utility class:
  - Current user retrieval
  - Role checking
  - Permission validation
- Comprehensive security documentation (package-info.java)

### Security
- JWT authentication with RS256 signing algorithm
- Stateless session management
- CSRF protection
- CORS configuration
- Security headers for XSS, clickjacking prevention
- Role-based access control
- Token expiration management
