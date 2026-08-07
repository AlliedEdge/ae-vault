package com.ziboto.backend.file.entity;

import com.ziboto.backend.common.entity.BaseEntity;
import com.ziboto.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a folder in the file system hierarchy.
 * Supports nested folder structures with parent-child relationships.
 */
@Entity
@Table(name = "folders", 
    uniqueConstraints = {
        @UniqueConstraint(
            name = "unique_folder_per_user",
            columnNames = {"user_id", "parent_folder_id", "folder_name", "deleted_at"}
        )
    },
    indexes = {
        @Index(name = "idx_folders_user_id", columnList = "user_id"),
        @Index(name = "idx_folders_parent_folder_id", columnList = "parent_folder_id"),
        @Index(name = "idx_folders_deleted_at", columnList = "deleted_at")
    }
)
@SQLDelete(sql = "UPDATE folders SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @Column(name = "parent_folder_id")
    private UUID parentFolderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id", insertable = false, updatable = false)
    private Folder parentFolder;
    
    @Column(name = "folder_name", nullable = false)
    private String folderName;
    
    @Column(name = "folder_path", nullable = false, columnDefinition = "TEXT")
    private String folderPath;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "last_modified_by")
    private String lastModifiedBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this is a root folder (no parent).
     */
    public boolean isRoot() {
        return parentFolderId == null;
    }
}
