package com.ziboto.backend.file.service;

import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.file.dto.FileMetadataResponse;
import com.ziboto.backend.file.dto.FileUploadResponse;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.entity.Folder;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.file.repository.FolderRepository;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import com.ziboto.backend.user.service.StorageUsageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service for file management operations.
 * Handles file upload, download, deletion, and metadata management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    
    private final FileMetadataRepository fileMetadataRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StorageUsageService storageUsageService;
    
    @Value("${app.storage.local.base-path:/var/ziboto/storage}")
    private String storagePath;
    
    @Value("${app.file.max-size:524288000}") // 500MB default
    private long maxFileSize;
    
    @Value("${app.file.allowed-types:}")
    private String allowedMimeTypes;
    
    private static final long MAX_FILE_SIZE_STANDARD = 100 * 1024 * 1024; // 100MB
    
    /**
     * Upload a file.
     * For files >100MB, client should use multipart upload.
     * 
     * @param file Multipart file
     * @param userId User ID
     * @param folderId Folder ID (optional)
     * @param username Username for audit
     * @return File upload response
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, Long userId, UUID folderId, String username) {
        log.info("File upload request - user: {}, folder: {}, file: {}, size: {}", 
                 userId, folderId, file.getOriginalFilename(), file.getSize());
        
        try {
            // 1. Validate file
            validateFile(file);
            
            // 2. Check storage quota
            checkStorageQuota(userId, file.getSize());
            
            // 3. Verify folder ownership if specified
            if (folderId != null) {
                verifyFolderOwnership(folderId, userId);
            }
            
            // 4. Calculate SHA-256 hash
            String sha256Hash = calculateSHA256(file);
            
            // 5. Check for duplicate filename in the same folder and rename if needed
            String fileName = file.getOriginalFilename();
            String uniqueFileName = generateUniqueFileName(userId, folderId, fileName);
            
            // 6. Calculate storage key first (without saving to DB yet)
            // Using a temporary UUID for storage, will be replaced by DB-generated ID
            UUID tempStorageId = UUID.randomUUID();
            
            // 7. Upload to storage with temporary ID
            String storageKey = storageService.uploadFile(userId, tempStorageId, file);
            
            // 8. Extract file metadata
            String fileExtension = getFileExtension(uniqueFileName);
            
            // 9. Create metadata entity WITHOUT setting ID (let JPA generate it)
            FileMetadata metadata = FileMetadata.builder()
                    .userId(userId)
                    .folderId(folderId)
                    .fileName(uniqueFileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .fileExtension(fileExtension)
                    .sha256Hash(sha256Hash)
                    .storageKey(storageKey)
                    .downloadCount(0)
                    .createdBy(username)
                    .lastModifiedBy(username)
                    .build();
            
            // Timestamps will be set automatically by @PrePersist
            
            // 10. Save to database FIRST
            log.debug("Saving file metadata to database - fileName: {}", uniqueFileName);
            FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
            log.debug("File metadata saved successfully - id: {}", savedMetadata.getId());
            
            // 11. Build response AFTER successful save
            FileUploadResponse response = FileUploadResponse.builder()
                    .fileId(savedMetadata.getId())
                    .fileName(savedMetadata.getFileName())
                    .fileSize(savedMetadata.getFileSize())
                    .formattedFileSize(formatBytes(savedMetadata.getFileSize()))
                    .mimeType(savedMetadata.getMimeType())
                    .fileExtension(savedMetadata.getFileExtension())
                    .sha256Hash(savedMetadata.getSha256Hash())
                    .uploadedAt(savedMetadata.getCreatedAt())
                    .folderId(savedMetadata.getFolderId())
                    .storageKey(savedMetadata.getStorageKey())
                    .isDuplicate(false)
                    .build();
            
            // 12. Update user storage
            updateUserStorage(userId, file.getSize());
            
            log.info("File uploaded successfully - fileId: {}, storageKey: {}", savedMetadata.getId(), storageKey);
            
            return response;
            
        } catch (Exception e) {
            log.error("File upload failed - user: {}, file: {}, error: {}", 
                     userId, file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Download a file.
     * Streams file directly from S3 to response.
     * 
     * @param fileId File ID
     * @param userId User ID
     * @param response HTTP response
     */
    @Transactional
    public void downloadFile(UUID fileId, Long userId, HttpServletResponse response) {
        log.info("File download request - fileId: {}, userId: {}", fileId, userId);
        
        // 1. Get file metadata
        FileMetadata metadata = fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // 2. Set response headers
        response.setContentType(metadata.getMimeType());
        response.setHeader("Content-Disposition", 
                          String.format("attachment; filename=\"%s\"", metadata.getFileName()));
        response.setContentLengthLong(metadata.getFileSize());
        
        // 3. Stream from storage
        try (InputStream storageStream = storageService.getFileStream(metadata.getStorageKey());
             OutputStream responseStream = response.getOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = storageStream.read(buffer)) != -1) {
                responseStream.write(buffer, 0, bytesRead);
            }
            responseStream.flush();
            
            log.info("File downloaded successfully - fileId: {}", fileId);
            
        } catch (IOException e) {
            log.error("Error streaming file: {}", e.getMessage(), e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to download file");
        }
        
        // 4. Increment download counter (async would be better)
        fileMetadataRepository.incrementDownloadCount(fileId);
    }
    
    /**
     * Get file metadata.
     * 
     * @param fileId File ID
     * @param userId User ID
     * @return File metadata response
     */
    @Transactional(readOnly = true)
    public FileMetadataResponse getFileMetadata(UUID fileId, Long userId) {
        FileMetadata metadata = fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        return buildMetadataResponse(metadata);
    }
    
    /**
     * List files in a folder.
     * 
     * @param userId User ID
     * @param folderId Folder ID (null for root)
     * @param pageable Pagination
     * @return Page of file metadata
     */
    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> listFiles(Long userId, UUID folderId, Pageable pageable) {
        Page<FileMetadata> files;
        
        if (folderId == null) {
            files = fileMetadataRepository.findByUserIdAndFolderIdIsNull(userId, pageable);
        } else {
            // Verify folder ownership
            verifyFolderOwnership(folderId, userId);
            files = fileMetadataRepository.findByUserIdAndFolderId(userId, folderId, pageable);
        }
        
        return files.map(this::buildMetadataResponse);
    }
    
    /**
     * Search files by name.
     * 
     * @param userId User ID
     * @param query Search query
     * @param pageable Pagination
     * @return Page of file metadata
     */
    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> searchFiles(Long userId, String query, Pageable pageable) {
        Page<FileMetadata> files = fileMetadataRepository.searchByFileName(userId, query, pageable);
        return files.map(this::buildMetadataResponse);
    }
    
    /**
     * Delete a file.
     * 
     * @param fileId File ID
     * @param userId User ID
     */
    @Transactional
    public void deleteFile(UUID fileId, Long userId) {
        log.info("File deletion request - fileId: {}, userId: {}", fileId, userId);
        
        // 1. Get file metadata
        FileMetadata metadata = fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // 2. Delete from storage
        storageService.deleteFile(metadata.getStorageKey());
        
        // 3. Soft delete from database
        fileMetadataRepository.delete(metadata);
        
        // 4. Update user storage
        updateUserStorage(userId, -metadata.getFileSize());
        
        log.info("File deleted successfully - fileId: {}", fileId);
    }
    
    /**
     * Validate uploaded file.
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "File is empty");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE_STANDARD) {
            throw new BaseException(ErrorCode.BAD_REQUEST, 
                    "File too large. Use multipart upload for files >100MB");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new BaseException(ErrorCode.BAD_REQUEST, 
                    String.format("File size exceeds maximum allowed size of %d MB", 
                                 maxFileSize / (1024 * 1024)));
        }
        
        // Validate MIME type if whitelist is configured
        if (allowedMimeTypes != null && !allowedMimeTypes.isEmpty()) {
            Set<String> allowedTypes = new HashSet<>(Arrays.asList(allowedMimeTypes.split(",")));
            String mimeType = file.getContentType();
            
            if (mimeType == null || !allowedTypes.contains(mimeType)) {
                throw new BaseException(ErrorCode.BAD_REQUEST, 
                        "File type not allowed: " + mimeType);
            }
        }
        
        // Validate file name
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "File name is required");
        }
        
        if (fileName.length() > 255) {
            throw new BaseException(ErrorCode.BAD_REQUEST, 
                    "File name too long (max 255 characters)");
        }
    }
    
    /**
     * Check if user has enough storage quota.
     */
    private void checkStorageQuota(Long userId, long fileSize) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        
        if (user.getStorageUsed() + fileSize > user.getStorageQuota()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, 
                    "Storage quota exceeded. Please upgrade your plan or delete some files.");
        }
    }
    
    /**
     * Update user storage usage.
     */
    private void updateUserStorage(Long userId, long sizeChange) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        
        user.setStorageUsed(user.getStorageUsed() + sizeChange);
        userRepository.save(user);
        
        // Invalidate storage cache to reflect changes immediately
        storageUsageService.invalidateCache(userId);
        
        log.debug("Updated storage for user {}: {} bytes (cache invalidated)", userId, sizeChange);
    }
    
    /**
     * Verify folder ownership.
     */
    private void verifyFolderOwnership(UUID folderId, Long userId) {
        if (!folderRepository.findByIdAndUserId(folderId, userId).isPresent()) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, 
                    "You don't have permission to access this folder");
        }
    }
    
    /**
     * Calculate SHA-256 hash of file.
     */
    private String calculateSHA256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error calculating file hash: {}", e.getMessage(), e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Failed to process file");
        }
    }
    
    /**
     * Generate unique filename by checking for duplicates and adding counter if needed.
     * Examples: 
     *   - file.txt -> file.txt (if no duplicate)
     *   - file.txt -> file (1).txt (if duplicate exists)
     *   - file.txt -> file (2).txt (if file and file (1) exist)
     */
    private String generateUniqueFileName(Long userId, UUID folderId, String originalFileName) {
        String baseFileName = originalFileName;
        String extension = getFileExtension(originalFileName);
        
        // Remove extension from base name
        if (extension != null && !extension.isEmpty()) {
            baseFileName = originalFileName.substring(0, originalFileName.length() - extension.length());
        }
        
        // Check if original filename exists
        boolean exists = fileMetadataRepository
                .existsByUserIdAndFolderIdAndFileName(userId, folderId, originalFileName);
        
        if (!exists) {
            return originalFileName;
        }
        
        // Generate unique name with counter
        int counter = 1;
        String uniqueName;
        do {
            uniqueName = baseFileName + " (" + counter + ")" + (extension != null ? extension : "");
            counter++;
        } while (fileMetadataRepository.existsByUserIdAndFolderIdAndFileName(userId, folderId, uniqueName));
        
        log.info("Generated unique filename: {} -> {}", originalFileName, uniqueName);
        return uniqueName;
    }
    
    /**
     * Extract file extension from file name.
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return null;
    }
    
    /**
     * Build upload response DTO.
     */
    private FileUploadResponse buildUploadResponse(FileMetadata metadata, boolean isDuplicate) {
        // Access all fields directly without triggering any methods that might cause lazy loading
        return FileUploadResponse.builder()
                .fileId(metadata.getId())
                .fileName(metadata.getFileName())
                .fileSize(metadata.getFileSize())
                .formattedFileSize(formatBytes(metadata.getFileSize())) // Use local method instead of entity method
                .mimeType(metadata.getMimeType())
                .fileExtension(metadata.getFileExtension())
                .sha256Hash(metadata.getSha256Hash())
                .uploadedAt(metadata.getCreatedAt())
                .folderId(metadata.getFolderId())
                .storageKey(metadata.getStorageKey())
                .isDuplicate(isDuplicate)
                .build();
    }
    
    /**
     * Format bytes to human-readable size (local method to avoid entity method calls)
     */
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }
        
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Build metadata response DTO.
     */
    private FileMetadataResponse buildMetadataResponse(FileMetadata metadata) {
        FileMetadataResponse.OwnerInfo owner = null;
        
        if (metadata.getUser() != null) {
            User user = metadata.getUser();
            owner = FileMetadataResponse.OwnerInfo.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .build();
        }
        
        String folderPath = null;
        if (metadata.getFolder() != null) {
            folderPath = metadata.getFolder().getFolderPath();
        }
        
        return FileMetadataResponse.builder()
                .fileId(metadata.getId())
                .fileName(metadata.getFileName())
                .originalFileName(metadata.getOriginalFileName())
                .fileSize(metadata.getFileSize())
                .formattedFileSize(metadata.getFormattedFileSize())
                .mimeType(metadata.getMimeType())
                .fileExtension(metadata.getExtension())
                .sha256Hash(metadata.getSha256Hash())
                .uploadedAt(metadata.getCreatedAt())
                .lastModified(metadata.getUpdatedAt())
                .folderId(metadata.getFolderId())
                .folderPath(folderPath)
                .downloadCount(metadata.getDownloadCount())
                .owner(owner)
                .build();
    }
}
