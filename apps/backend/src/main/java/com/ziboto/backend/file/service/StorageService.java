package com.ziboto.backend.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * Storage service interface for file operations.
 * Abstracts storage implementation (local, S3, etc.).
 */
public interface StorageService {
    
    /**
     * Upload file to storage.
     * 
     * @param userId User ID
     * @param fileId File ID
     * @param file Multipart file
     * @return Storage key/path of uploaded file
     */
    String uploadFile(Long userId, UUID fileId, MultipartFile file);
    
    /**
     * Get file stream from storage.
     * 
     * @param storageKey Storage key/path
     * @return InputStream of the file
     */
    InputStream getFileStream(String storageKey);
    
    /**
     * Delete file from storage.
     * 
     * @param storageKey Storage key/path
     */
    void deleteFile(String storageKey);
    
    /**
     * Check if file exists in storage.
     * 
     * @param storageKey Storage key/path
     * @return true if file exists
     */
    boolean fileExists(String storageKey);
}
