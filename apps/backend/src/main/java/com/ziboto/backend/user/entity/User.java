package com.ziboto.backend.user.entity;

import com.ziboto.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(length = 100)
    private String firstName;
    
    @Column(length = 100)
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;
    
    private Boolean emailVerified;
    
    @Column(length = 500)
    private String avatarUrl;
    
    @Column(length = 50)
    private String timezone; // e.g., "America/New_York", "UTC", "Europe/London"
    
    @Column(length = 10)
    private String language; // e.g., "en", "es", "fr", ISO 639-1 codes
    
    private Long storageQuota; // in bytes
    
    private Long storageUsed; // in bytes
    
    private java.time.LocalDateTime lastLoginAt;
}
