package com.ziboto.backend.file.entity;

import com.ziboto.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing file metadata stored in the system.
 * The actual file content is stored in S3, this entity tracks metadata.
 */
@Entity
@Table(name = "file_metadata",
    indexes = {
        @Index(name = "idx_file_metadata_user_id", columnList = "user_id"),
        @Index(name = "idx_file_metadata_folder_id", columnList = "folder_id"),
        @Index(name = "idx_file_metadata_sha256_hash", columnList = "sha256_hash"),
        @Index(name = "idx_file_metadata_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_file_metadata_mime_type", columnList = "mime_type")
    }
)
@SQLDelete(sql = "UPDATE file_metadata SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @Column(name = "folder_id")
    private UUID folderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", insertable = false, updatable = false)
    private Folder folder;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;
    
    @Column(name = "file_extension", length = 20)
    private String fileExtension;
    
    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;
    
    @Column(name = "storage_key", nullable = false, length = 500, unique = true)
    private String storageKey;
    
    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;
    
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
     * Increment download count (thread-safe via database).
     */
    public void incrementDownloadCount() {
        if (this.downloadCount == null) {
            this.downloadCount = 0;
        }
        this.downloadCount++;
    }
    
    /**
     * Get human-readable file size.
     */
    public String getFormattedFileSize() {
        if (fileSize == null) {
            return "0 B";
        }
        
        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Get file extension from fileName.
     */
    public String getExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
