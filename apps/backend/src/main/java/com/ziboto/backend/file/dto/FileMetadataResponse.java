package com.ziboto.backend.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for file metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadataResponse {
    private UUID fileId;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
    private String fileExtension;
    private String sha256Hash;
    private LocalDateTime uploadedAt;
    private LocalDateTime lastModified;
    private UUID folderId;
    private String folderPath;
    private Integer downloadCount;
    private OwnerInfo owner;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OwnerInfo {
        private Long userId;
        private String email;
        private String name;
    }
}
