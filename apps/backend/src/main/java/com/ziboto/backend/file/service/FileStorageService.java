package com.ziboto.backend.file.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    
    String store(MultipartFile file, String bucketName, String fileName);
    
    Resource load(String storageKey);
    
    InputStream loadAsStream(String storageKey);
    
    void delete(String storageKey);
    
    boolean exists(String storageKey);
}
