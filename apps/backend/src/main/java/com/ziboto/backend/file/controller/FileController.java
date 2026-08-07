package com.ziboto.backend.file.controller;

import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.file.dto.FileMetadataResponse;
import com.ziboto.backend.file.dto.FileUploadResponse;
import com.ziboto.backend.file.service.FileService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for file management operations.
 * Handles file upload, download, deletion, and metadata retrieval.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "APIs for file upload, download, and management")
public class FileController {
    
    private final FileService fileService;
    private final UserRepository userRepository;
    
    /**
     * Upload a file.
     * For files >100MB, use multipart upload endpoints instead.
     *
     * @param file The file to upload
     * @param folderId Optional folder ID (null for root)
     * @param authentication Current user authentication
     * @return File upload response
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file", 
               description = "Upload a file (max 100MB). For larger files, use multipart upload.")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            Authentication authentication) {
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = getUserId(authentication);
        
        log.info("File upload request - user: {}, file: {}", 
                 userDetails.getUsername(), file.getOriginalFilename());
        
        FileUploadResponse response = fileService.uploadFile(
                file, userId, folderId, userDetails.getUsername());
        
        return ApiResponse.success("File uploaded successfully", response);
    }
    
    /**
     * Download a file.
     *
     * @param fileId File ID
     * @param authentication Current user authentication
     * @param response HTTP response
     */
    @GetMapping("/{fileId}/download")
    @Operation(summary = "Download a file", description = "Download a file by ID")
    public void downloadFile(
            @PathVariable UUID fileId,
            Authentication authentication,
            HttpServletResponse response) {
        
        Long userId = getUserId(authentication);
        log.info("File download request - fileId: {}, userId: {}", fileId, userId);
        
        fileService.downloadFile(fileId, userId, response);
    }
    
    /**
     * Get file metadata.
     *
     * @param fileId File ID
     * @param authentication Current user authentication
     * @return File metadata
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata", description = "Retrieve file metadata by ID")
    public ApiResponse<FileMetadataResponse> getFileMetadata(
            @PathVariable UUID fileId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        FileMetadataResponse response = fileService.getFileMetadata(fileId, userId);
        
        return ApiResponse.success(response);
    }
    
    /**
     * List files in a folder.
     *
     * @param folderId Optional folder ID (null for root)
     * @param pageable Pagination parameters
     * @param authentication Current user authentication
     * @return Page of files
     */
    @GetMapping
    @Operation(summary = "List files", description = "List files in a folder (or root if no folder specified)")
    public ApiResponse<Page<FileMetadataResponse>> listFiles(
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        Page<FileMetadataResponse> files = fileService.listFiles(userId, folderId, pageable);
        
        return ApiResponse.success(files);
    }
    
    /**
     * Search files by name.
     *
     * @param query Search query
     * @param pageable Pagination parameters
     * @param authentication Current user authentication
     * @return Page of matching files
     */
    @GetMapping("/search")
    @Operation(summary = "Search files", description = "Search files by name")
    public ApiResponse<Page<FileMetadataResponse>> searchFiles(
            @RequestParam("q") String query,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        Page<FileMetadataResponse> files = fileService.searchFiles(userId, query, pageable);
        
        return ApiResponse.success(files);
    }
    
    /**
     * Delete a file.
     *
     * @param fileId File ID
     * @param authentication Current user authentication
     * @return Success response
     */
    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete a file", description = "Permanently delete a file")
    public ApiResponse<Void> deleteFile(
            @PathVariable UUID fileId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("File deletion request - fileId: {}, userId: {}", fileId, userId);
        
        fileService.deleteFile(fileId, userId);
        
        return ApiResponse.success("File deleted successfully", null);
    }
    
    /**
     * Extract user ID from authentication.
     * Fetches the user from database using username from JWT token.
     */
    private Long getUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with username: " + username
                ));
        
        return user.getId();
    }
}
