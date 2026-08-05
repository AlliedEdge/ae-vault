package com.ziboto.backend.user.dto;

import com.ziboto.backend.user.entity.UserRole;
import com.ziboto.backend.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private UserStatus status;
    private Boolean emailVerified;
    private String avatarUrl;
    private String timezone;
    private String language;
    private Long storageQuota;
    private Long storageUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
