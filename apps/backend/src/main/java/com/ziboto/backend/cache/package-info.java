/**
 * Redis caching infrastructure for Ziboto.
 * 
 * <h2>Overview</h2>
 * <p>This package provides Redis-based caching with Spring Data Redis integration.
 * It includes configuration, general-purpose caching, and specialized authentication services.</p>
 * 
 * <h2>Components</h2>
 * 
 * <h3>RedisConfig</h3>
 * <p>Spring configuration for Redis:</p>
 * <ul>
 *   <li>RedisTemplate configuration with JSON serialization</li>
 *   <li>CacheManager with custom TTL settings</li>
 *   <li>Connection pooling via Lettuce</li>
 *   <li>Automatic type handling with Jackson</li>
 * </ul>
 * 
 * <h3>CacheService</h3>
 * <p>General-purpose caching service:</p>
 * <ul>
 *   <li>Get/Set operations with automatic serialization</li>
 *   <li>TTL management and expiration</li>
 *   <li>Pattern-based deletion (use with caution)</li>
 *   <li>Increment/Decrement for counters</li>
 *   <li>Key existence checks</li>
 * </ul>
 * 
 * <h2>Authentication Services</h2>
 * <p>Specialized Redis services for authentication (in auth.service package):</p>
 * <ul>
 *   <li>{@link com.ziboto.backend.auth.service.RateLimitService} - Rate limiting</li>
 *   <li>{@link com.ziboto.backend.auth.service.FailedLoginAttemptService} - Login attempt tracking</li>
 *   <li>{@link com.ziboto.backend.auth.service.TokenBlacklistService} - JWT revocation</li>
 *   <li>{@link com.ziboto.backend.auth.service.SessionCacheService} - Session caching</li>
 * </ul>
 * 
 * <h2>Configuration</h2>
 * <pre>
 * spring:
 *   data:
 *     redis:
 *       host: ${REDIS_HOST:localhost}
 *       port: ${REDIS_PORT:6379}
 *       password: ${REDIS_PASSWORD:}
 *       database: ${REDIS_DATABASE:0}
 * 
 * app:
 *   cache:
 *     ttl: ${CACHE_TTL:3600}
 *     prefix: "ziboto"
 * </pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Service
 * public class UserService {
 *     
 *     @Autowired
 *     private CacheService cacheService;
 *     
 *     public User getUser(Long id) {
 *         // Try cache first
 *         User cached = cacheService.get("user:" + id, User.class);
 *         if (cached != null) {
 *             return cached;
 *         }
 *         
 *         // Load from database
 *         User user = userRepository.findById(id).orElseThrow();
 *         
 *         // Cache for 1 hour
 *         cacheService.set("user:" + id, user, Duration.ofHours(1));
 *         
 *         return user;
 *     }
 * }
 * }</pre>
 * 
 * <h2>Key Patterns</h2>
 * <ul>
 *   <li>rate_limit:login:{identifier} - Login rate limiting</li>
 *   <li>rate_limit:api:{userId} - API rate limiting</li>
 *   <li>failed_login:attempts:{identifier} - Failed login counter</li>
 *   <li>failed_login:lockout:{identifier} - Account lockout flag</li>
 *   <li>token:blacklist:{token} - Blacklisted JWT tokens</li>
 *   <li>token:user_blacklist:{username} - User token invalidation</li>
 *   <li>session:user:{username} - Cached user sessions</li>
 *   <li>session:meta:{sessionId} - Session metadata</li>
 *   <li>session:active:{username} - Active sessions tracking</li>
 * </ul>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Always set TTL for cached data to prevent memory leaks</li>
 *   <li>Use hierarchical key naming (prefix:category:identifier)</li>
 *   <li>Handle Redis failures gracefully (fail open for non-critical features)</li>
 *   <li>Monitor cache hit ratios and memory usage</li>
 *   <li>Use connection pooling for better performance</li>
 *   <li>Avoid storing sensitive data without encryption</li>
 * </ul>
 * 
 * @see com.ziboto.backend.cache.RedisConfig
 * @see com.ziboto.backend.cache.CacheService
 * @see com.ziboto.backend.auth.service.RateLimitService
 * @see com.ziboto.backend.auth.service.TokenBlacklistService
 */
package com.ziboto.backend.cache;
