# Changelog - Authentication Module

All notable changes to the authentication module are documented here.

## [Unreleased]

### Changed
- Enhanced AuthServiceImpl with improved error handling
- Updated RateLimitService for better rate limiting logic

---

## [0.2.0] - 2026-08-05

### Added
- AuthController with comprehensive authentication endpoints:
  - POST /api/v1/auth/register - User registration
  - POST /api/v1/auth/login - User login
  - POST /api/v1/auth/refresh - Token refresh
  - POST /api/v1/auth/logout - User logout
  - GET /api/v1/auth/verify - Token verification
- AuthService interface and AuthServiceImpl with:
  - User registration with validation
  - Login with credentials verification
  - Refresh token generation and rotation
  - Logout with token blacklist
  - Token verification
- RefreshToken entity with expiration management
- RefreshTokenRepository with custom queries
- RefreshTokenService for token lifecycle management
- RegistrationService and RegistrationServiceImpl
- CustomUserDetailsService for Spring Security integration
- Authentication DTOs:
  - LoginRequest
  - RegisterRequest
  - RefreshTokenRequest
  - AuthResponse
  - AuthenticationResponse
  - VerifyTokenResponse
- AuthMapper for DTO conversions
- Token blacklist service with Redis
- Session cache service with Redis
- OTP cache service for email verification
- Failed login attempt tracking service
- Rate limiting service with configurable limits
- Comprehensive validation and error handling

### Security
- JWT token generation with RS256 algorithm
- Refresh token rotation on each use
- Token blacklist for logout
- Failed login attempt tracking
- Account lockout after 5 failed attempts
- Rate limiting per endpoint
- Session management with Redis
- Password validation rules
