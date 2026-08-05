package com.ziboto.backend.user.dto;

import com.ziboto.backend.user.entity.UserRole;
import com.ziboto.backend.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Comprehensive User DTO for data transfer.
 * 
 * <p>Contains all user information including metadata.
 * This DTO can be used for detailed user information display
 * and administrative operations.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private UserStatus status;
    private Boolean emailVerified;
    private String avatarUrl;
    private Long storageQuota;
    private Long storageUsed;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String lastModifiedBy;
}
