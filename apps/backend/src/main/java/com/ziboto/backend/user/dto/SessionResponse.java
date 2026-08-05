package com.ziboto.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user session information.
 * 
 * <p>Represents an active refresh token session with device and location information.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    
    private String id;                  // Session ID (refresh token ID)
    private String deviceInfo;          // Device information
    private String ipAddress;           // IP address
    private String userAgent;           // User agent string
    private LocalDateTime createdAt;    // Session creation time
    private LocalDateTime lastUsedAt;   // Last time session was used
    private LocalDateTime expiresAt;    // Session expiration time
    private Boolean current;            // Is this the current session
}
