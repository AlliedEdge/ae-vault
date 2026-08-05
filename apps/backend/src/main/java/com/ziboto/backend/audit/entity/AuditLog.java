package com.ziboto.backend.audit.entity;

import com.ziboto.backend.common.entity.BaseEntity;
import com.ziboto.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_entity_type", columnList = "entity_type"),
        @Index(name = "idx_action", columnList = "action"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false, length = 50)
    private String entityType; // User, Bucket, File, etc.
    
    @Column(nullable = false)
    private Long entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;
    
    @Column(columnDefinition = "TEXT")
    private String details; // JSON or text description
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(length = 255)
    private String userAgent;
}
