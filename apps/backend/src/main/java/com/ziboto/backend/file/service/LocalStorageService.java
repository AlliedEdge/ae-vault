package com.ziboto.backend.file.service;

import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem storage service.
 * Stores files on the local filesystem for development/testing.
 */
@Service
@Slf4j
public class LocalStorageService implements StorageService {
    
    @Value("${app.storage.local.base-path:/var/ziboto/storage}")
    private String basePath;
    
    /**
     * Upload file to local storage.
     * 
     * @param userId User ID
     * @param fileId File ID
     * @param file Multipart file
     * @return Storage path of uploaded file
     */
    @Override
    public String uploadFile(Long userId, UUID fileId, MultipartFile file) {
        try {
            String storagePath = generateStoragePath(userId, fileId, file.getOriginalFilename());
            Path fullPath = Paths.get(basePath, storagePath);
            
            // Create parent directories if they don't exist
            Files.createDirectories(fullPath.getParent());
            
            log.info("Uploading file to local storage - path: {}, size: {}", 
                     fullPath, file.getSize());
            
            // Copy file to destination
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, fullPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            log.info("File uploaded successfully to local storage: {}", storagePath);
            return storagePath;
            
        } catch (IOException e) {
            log.error("Error uploading file to local storage: {}", e.getMessage(), e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, 
                                   "Failed to upload file to local storage");
        }
    }
    
    /**
     * Get file stream from local storage.
     * 
     * @param storageKey Storage path
     * @return InputStream of the file
     */
    @Override
    public InputStream getFileStream(String storageKey) {
        try {
            Path fullPath = Paths.get(basePath, storageKey);
            
            if (!Files.exists(fullPath)) {
                log.error("File not found in local storage: {}", fullPath);
                throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found in storage");
            }
            
            log.debug("Retrieving file from local storage: {}", fullPath);
            return Files.newInputStream(fullPath);
            
        } catch (IOException e) {
            log.error("Error reading file from local storage: {}", e.getMessage(), e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, 
                                   "Failed to retrieve file from storage");
        }
    }
    
    /**
     * Delete file from local storage.
     * 
     * @param storageKey Storage path
     */
    @Override
    public void deleteFile(String storageKey) {
        try {
            Path fullPath = Paths.get(basePath, storageKey);
            
            if (Files.exists(fullPath)) {
                log.info("Deleting file from local storage: {}", fullPath);
                Files.delete(fullPath);
                log.info("File deleted successfully from local storage: {}", storageKey);
            } else {
                log.warn("File not found in local storage, skipping deletion: {}", fullPath);
            }
            
        } catch (IOException e) {
            log.error("Error deleting file from local storage: {}", e.getMessage(), e);
            // Don't throw exception for deletion failures
            log.warn("Failed to delete file from local storage, continuing: {}", storageKey);
        }
    }
    
    /**
     * Check if file exists in local storage.
     * 
     * @param storageKey Storage path
     * @return true if file exists
     */
    @Override
    public boolean fileExists(String storageKey) {
        Path fullPath = Paths.get(basePath, storageKey);
        boolean exists = Files.exists(fullPath);
        log.debug("File exists check: {} -> {}", fullPath, exists);
        return exists;
    }
    
    /**
     * Generate storage path for file.
     * Format: users/{userId}/files/{fileId}/{sanitizedFileName}
     * 
     * @param userId User ID
     * @param fileId File ID
     * @param originalFileName Original file name
     * @return Storage path
     */
    private String generateStoragePath(Long userId, UUID fileId, String originalFileName) {
        String sanitizedFileName = sanitizeFileName(originalFileName);
        return String.format("users/%d/files/%s/%s", userId, fileId, sanitizedFileName);
    }
    
    /**
     * Sanitize file name to prevent path traversal and special characters.
     * 
     * @param fileName Original file name
     * @return Sanitized file name
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "file";
        }
        
        // Remove path separators and special characters
        String sanitized = fileName.replaceAll("[/\\\\:*?\"<>|]", "_");
        
        // Limit length
        if (sanitized.length() > 255) {
            String extension = "";
            int dotIndex = sanitized.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = sanitized.substring(dotIndex);
                sanitized = sanitized.substring(0, Math.min(255 - extension.length(), dotIndex));
            } else {
                sanitized = sanitized.substring(0, 255);
            }
            sanitized = sanitized + extension;
        }
        
        return sanitized;
    }
}
