/**
 * Security infrastructure for JWT-based authentication.
 * 
 * <h2>Overview</h2>
 * <p>This package contains the JWT token infrastructure for the Ziboto backend.
 * It provides token generation, validation, and Spring Security integration.</p>
 * 
 * <h2>Components</h2>
 * 
 * <h3>JwtProperties</h3>
 * <p>Configuration properties for JWT tokens loaded from application.yml:</p>
 * <ul>
 *   <li><b>JWT_SECRET</b> - Secret key for signing tokens (Base64, min 256 bits)</li>
 *   <li><b>JWT_EXPIRATION</b> - Access token expiration (default: 900000ms = 15 minutes)</li>
 *   <li><b>JWT_REFRESH_EXPIRATION</b> - Refresh token expiration (default: 604800000ms = 7 days)</li>
 * </ul>
 * 
 * <h3>JwtTokenProvider</h3>
 * <p>Core JWT functionality:</p>
 * <ul>
 *   <li>Generate access tokens (15 minutes)</li>
 *   <li>Generate refresh tokens (7 days)</li>
 *   <li>Validate token signatures</li>
 *   <li>Extract user information (username, roles, ID)</li>
 *   <li>Check token expiration</li>
 *   <li>Handle expired tokens gracefully</li>
 * </ul>
 * 
 * <h3>JwtAuthenticationFilter</h3>
 * <p>Spring Security filter that:</p>
 * <ul>
 *   <li>Intercepts HTTP requests</li>
 *   <li>Extracts JWT from Authorization header (Bearer token)</li>
 *   <li>Validates token signature and expiration</li>
 *   <li>Loads user details from database</li>
 *   <li>Sets authentication in SecurityContext</li>
 *   <li>Skips public endpoints (login, register, etc.)</li>
 * </ul>
 * 
 * <h3>JwtAuthenticationEntryPoint</h3>
 * <p>Handles authentication failures:</p>
 * <ul>
 *   <li>Returns JSON error responses (not HTML)</li>
 *   <li>Provides specific error messages for different failure types</li>
 *   <li>Distinguishes between expired, invalid, and malformed tokens</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Generate Tokens</h3>
 * <pre>{@code
 * @Service
 * public class AuthService {
 *     
 *     @Autowired
 *     private JwtTokenProvider tokenProvider;
 *     
 *     public AuthenticationResponse login(LoginRequest request) {
 *         Authentication auth = authenticationManager.authenticate(...);
 *         
 *         String accessToken = tokenProvider.generateToken(auth);
 *         String refreshToken = tokenProvider.generateRefreshToken(auth);
 *         
 *         return AuthenticationResponse.builder()
 *                 .accessToken(accessToken)
 *                 .refreshToken(refreshToken)
 *                 .expiresIn(900L) // 15 minutes
 *                 .build();
 *     }
 * }
 * }</pre>
 * 
 * <h3>Validate Tokens</h3>
 * <pre>{@code
 * if (tokenProvider.validateAccessToken(token)) {
 *     String username = tokenProvider.getUsernameFromToken(token);
 *     Iterable<String> roles = tokenProvider.getRolesFromToken(token);
 *     // Process authenticated request
 * }
 * }</pre>
 * 
 * <h3>Client Usage</h3>
 * <pre>{@code
 * // Login request
 * POST /api/v1/auth/login
 * {
 *   "usernameOrEmail": "john_doe",
 *   "password": "SecurePass123"
 * }
 * 
 * // Response
 * {
 *   "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
 *   "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 900
 * }
 * 
 * // Authenticated request
 * GET /api/v1/user/me
 * Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
 * }</pre>
 * 
 * <h2>Configuration</h2>
 * 
 * <h3>application.yml</h3>
 * <pre>
 * app:
 *   security:
 *     jwt:
 *       secret: ${JWT_SECRET:}
 *       expiration: ${JWT_EXPIRATION:900000}
 *       refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}
 * </pre>
 * 
 * <h3>Environment Variables (.env)</h3>
 * <pre>
 * # Generate a secure secret key (Base64 encoded, min 256 bits):
 * # openssl rand -base64 32
 * JWT_SECRET=your-base64-encoded-secret-key-here
 * JWT_EXPIRATION=900000
 * JWT_REFRESH_EXPIRATION=604800000
 * </pre>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>JWT secret MUST be at least 256 bits (32 bytes) for HS512</li>
 *   <li>Store JWT secret in environment variables, never in code</li>
 *   <li>Access tokens have short expiration (15 minutes) to limit exposure</li>
 *   <li>Refresh tokens have longer expiration (7 days) and should be stored securely</li>
 *   <li>Tokens are signed with HS512 algorithm for strong security</li>
 *   <li>Token validation includes signature, expiration, and structure checks</li>
 * </ul>
 * 
 * <h2>Token Structure</h2>
 * 
 * <h3>Access Token Claims</h3>
 * <pre>{@code
 * {
 *   "sub": "john_doe",           // Username
 *   "roles": ["ROLE_USER"],       // User roles
 *   "type": "access",             // Token type
 *   "iss": "ziboto",              // Issuer
 *   "aud": "ziboto-api",          // Audience
 *   "iat": 1723041234,            // Issued at
 *   "exp": 1723042134             // Expires at (15 min later)
 * }
 * }</pre>
 * 
 * <h3>Refresh Token Claims</h3>
 * <pre>{@code
 * {
 *   "sub": "john_doe",           // Username
 *   "type": "refresh",            // Token type
 *   "iss": "ziboto",              // Issuer
 *   "aud": "ziboto-api",          // Audience
 *   "iat": 1723041234,            // Issued at
 *   "exp": 1723646034             // Expires at (7 days later)
 * }
 * }</pre>
 * 
 * <h2>Error Responses</h2>
 * <ul>
 *   <li><b>401 Unauthorized</b> - No token provided or invalid token</li>
 *   <li><b>401 Token Expired</b> - Token has expired, refresh needed</li>
 *   <li><b>401 Invalid Signature</b> - Token signature verification failed</li>
 *   <li><b>401 Malformed Token</b> - Token structure is invalid</li>
 * </ul>
 * 
 * @see com.ziboto.backend.security.JwtTokenProvider
 * @see com.ziboto.backend.security.JwtAuthenticationFilter
 * @see com.ziboto.backend.security.JwtAuthenticationEntryPoint
 * @see com.ziboto.backend.security.JwtProperties
 */
package com.ziboto.backend.security;
