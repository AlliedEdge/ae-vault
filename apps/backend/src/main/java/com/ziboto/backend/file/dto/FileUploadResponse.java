package com.ziboto.backend.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for file upload operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    private UUID fileId;
    private String fileName;
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
    private String fileExtension;
    private String sha256Hash;
    private LocalDateTime uploadedAt;
    private UUID folderId;
    private String storageKey;
    private Boolean isDuplicate;
}
