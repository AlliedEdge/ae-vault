/**
 * Repository layer for the Authentication module.
 * 
 * <p>This package contains Spring Data JPA repositories for authentication-related entities.</p>
 * 
 * <h2>Repositories:</h2>
 * <ul>
 *   <li>{@link com.ziboto.backend.auth.repository.RefreshTokenRepository} - Manages refresh token persistence and queries</li>
 * </ul>
 * 
 * <h2>User Repository:</h2>
 * <p>
 * The {@code UserRepository} is located in the user module at:
 * {@link com.ziboto.backend.user.repository.UserRepository}
 * </p>
 * <p>
 * This follows domain-driven design principles where the User entity belongs to the user module,
 * while authentication-specific entities (like RefreshToken) belong to the auth module.
 * </p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Service
 * public class AuthServiceImpl implements AuthService {
 *     
 *     private final UserRepository userRepository;
 *     private final RefreshTokenRepository refreshTokenRepository;
 *     
 *     public AuthServiceImpl(UserRepository userRepository,
 *                            RefreshTokenRepository refreshTokenRepository) {
 *         this.userRepository = userRepository;
 *         this.refreshTokenRepository = refreshTokenRepository;
 *     }
 *     
 *     // Service methods...
 * }
 * }</pre>
 * 
 * @see com.ziboto.backend.auth.entity.RefreshToken
 * @see com.ziboto.backend.user.repository.UserRepository
 * @see com.ziboto.backend.user.entity.User
 */
package com.ziboto.backend.auth.repository;
