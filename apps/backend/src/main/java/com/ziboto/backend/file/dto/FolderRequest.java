package com.ziboto.backend.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for folder creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderRequest {
    
    @NotBlank(message = "Folder name is required")
    @Size(min = 1, max = 255, message = "Folder name must be between 1 and 255 characters")
    @Pattern(regexp = "^[^/\\\\:*?\"<>|]+$", 
             message = "Folder name cannot contain special characters: / \\ : * ? \" < > |")
    private String folderName;
    
    private UUID parentFolderId; // null for root folder
}
