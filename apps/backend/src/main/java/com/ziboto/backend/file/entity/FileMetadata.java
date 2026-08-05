package com.ziboto.backend.file.entity;

import com.ziboto.backend.common.entity.BaseEntity;
import com.ziboto.backend.storage.entity.Bucket;
import com.ziboto.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata extends BaseEntity {
    
    @Column(nullable = false, length = 255)
    private String fileName;
    
    @Column(nullable = false, length = 500)
    private String filePath;
    
    @Column(nullable = false)
    private Long fileSize; // in bytes
    
    @Column(length = 100)
    private String contentType;
    
    @Column(length = 64)
    private String checksum; // MD5 or SHA-256
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", nullable = false)
    private Bucket bucket;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User uploader;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status;
    
    @Column(length = 500)
    private String storageKey; // Key in storage system (S3, etc.)
}
