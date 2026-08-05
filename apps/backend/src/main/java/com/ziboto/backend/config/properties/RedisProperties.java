package com.ziboto.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis configuration properties for rate limiting, caching, and session management.
 * 
 * <p>All Redis-related TTL and limit configurations are centralized here.
 * Values can be overridden via environment variables or application.yml.</p>
 * 
 * <h2>Configuration Sections:</h2>
 * <ul>
 *   <li>Rate Limiting - Login, signup, API, and token refresh limits</li>
 *   <li>Failed Login Attempts - Brute force protection</li>
 *   <li>Session Cache - User session data caching</li>
 *   <li>Token Blacklist - JWT revocation</li>
 *   <li>OTP - One-Time Password caching (future use)</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {
    
    private RateLimit rateLimit = new RateLimit();
    private FailedLogin failedLogin = new FailedLogin();
    private Session session = new Session();
    private TokenBlacklist tokenBlacklist = new TokenBlacklist();
    private Otp otp = new Otp();
    private KeyPrefix keyPrefix = new KeyPrefix();
    
    /**
     * Rate limiting configuration.
     */
    @Data
    public static class RateLimit {
        
        /** Login rate limiting */
        private Login login = new Login();
        
        /** Signup/registration rate limiting */
        private Signup signup = new Signup();
        
        /** General API rate limiting */
        private Api api = new Api();
        
        /** Token refresh rate limiting */
        private Refresh refresh = new Refresh();
        
        @Data
        public static class Login {
            /** Maximum login attempts per window */
            private int maxAttempts = 5;
            /** Time window in minutes */
            private int windowMinutes = 15;
        }
        
        @Data
        public static class Signup {
            /** Maximum signup attempts per window */
            private int maxAttempts = 3;
            /** Time window in minutes */
            private int windowMinutes = 60;
        }
        
        @Data
        public static class Api {
            /** Maximum API requests per window */
            private int maxRequests = 100;
            /** Time window in minutes */
            private int windowMinutes = 1;
        }
        
        @Data
        public static class Refresh {
            /** Maximum refresh attempts per window */
            private int maxAttempts = 10;
            /** Time window in hours */
            private int windowHours = 1;
        }
    }
    
    /**
     * Failed login attempt tracking configuration.
     */
    @Data
    public static class FailedLogin {
        /** Maximum failed attempts before lockout */
        private int maxAttempts = 5;
        /** Account lockout duration in minutes */
        private int lockoutMinutes = 30;
        /** Failed attempt tracking window in hours */
        private int trackingHours = 1;
    }
    
    /**
     * Session cache configuration.
     */
    @Data
    public static class Session {
        /** Default session cache TTL in hours */
        private int ttlHours = 1;
        /** Extended session cache TTL in hours (for metadata) */
        private int extendedTtlHours = 24;
        /** Enable sliding window expiration (refresh TTL on access) */
        private boolean slidingWindow = true;
        /** Maximum concurrent sessions per user (0 = unlimited) */
        private int maxConcurrentSessions = 0;
    }
    
    /**
     * Token blacklist configuration.
     */
    @Data
    public static class TokenBlacklist {
        /** Enable token blacklisting */
        private boolean enabled = true;
        /** Maximum blacklist TTL in days (should match longest token expiration) */
        private int maxTtlDays = 7;
    }
    
    /**
     * OTP (One-Time Password) configuration.
     */
    @Data
    public static class Otp {
        /** OTP validity duration in minutes */
        private int ttlMinutes = 5;
        /** Maximum OTP generation attempts per window */
        private int maxAttempts = 3;
        /** OTP generation rate limit window in minutes */
        private int rateLimitMinutes = 15;
        /** Maximum OTP verification attempts before invalidation */
        private int maxVerificationAttempts = 3;
    }
    
    /**
     * Redis key prefixes for namespace isolation.
     */
    @Data
    public static class KeyPrefix {
        private String rateLimit = "rate_limit";
        private String failedLogin = "failed_login";
        private String session = "session";
        private String tokenBlacklist = "token:blacklist";
        private String otp = "otp";
    }
}
