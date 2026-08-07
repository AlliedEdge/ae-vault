package com.ziboto.backend.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for folder operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResponse {
    private UUID folderId;
    private String folderName;
    private UUID parentFolderId;
    private String folderPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long fileCount;
    private Long subfolderCount;
}
