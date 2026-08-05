package com.ziboto.backend.file.service;

import com.ziboto.backend.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalFileStorageService implements FileStorageService {
    
    private final AppProperties appProperties;
    
    @Override
    public String store(MultipartFile file, String bucketName, String fileName) {
        // TODO: Implement local file storage
        return null;
    }
    
    @Override
    public Resource load(String storageKey) {
        // TODO: Implement file loading
        return null;
    }
    
    @Override
    public InputStream loadAsStream(String storageKey) {
        // TODO: Implement stream loading
        return null;
    }
    
    @Override
    public void delete(String storageKey) {
        // TODO: Implement file deletion
    }
    
    @Override
    public boolean exists(String storageKey) {
        // TODO: Implement existence check
        return false;
    }
}
