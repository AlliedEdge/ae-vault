package com.ziboto.backend.file.service;

import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.file.dto.FolderRequest;
import com.ziboto.backend.file.dto.FolderResponse;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.entity.Folder;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.file.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for folder management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FolderService {
    
    private final FolderRepository folderRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final StorageService storageService;
    
    private static final int MAX_FOLDER_DEPTH = 10;
    
    /**
     * Create a new folder.
     * 
     * @param request Folder creation request
     * @param userId User ID
     * @param username Username for audit
     * @return Folder response
     */
    @Transactional
    public FolderResponse createFolder(FolderRequest request, Long userId, String username) {
        log.info("Creating folder - user: {}, name: {}, parent: {}", 
                 userId, request.getFolderName(), request.getParentFolderId());
        
        // 1. Check if folder already exists
        if (folderRepository.existsByUserIdAndParentFolderIdAndFolderName(
                userId, request.getParentFolderId(), request.getFolderName())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, 
                    "A folder with this name already exists in the specified location");
        }
        
        // 2. Build folder path
        String folderPath = buildFolderPath(userId, request.getParentFolderId(), request.getFolderName());
        
        // 3. Check folder depth
        int depth = folderPath.split("/").length - 1; // -1 for leading /
        if (depth > MAX_FOLDER_DEPTH) {
            throw new BaseException(ErrorCode.BAD_REQUEST, 
                    String.format("Maximum folder depth of %d exceeded", MAX_FOLDER_DEPTH));
        }
        
        // 4. Create folder entity
        Folder folder = Folder.builder()
                .userId(userId)
                .parentFolderId(request.getParentFolderId())
                .folderName(request.getFolderName())
                .folderPath(folderPath)
                .createdBy(username)
                .lastModifiedBy(username)
                .build();
        
        // 5. Save to database
        folderRepository.save(folder);
        
        log.info("Folder created successfully - folderId: {}, path: {}", 
                 folder.getId(), folder.getFolderPath());
        
        return buildFolderResponse(folder);
    }
    
    /**
     * Get folder by ID.
     * 
     * @param folderId Folder ID
     * @param userId User ID
     * @return Folder response
     */
    @Transactional(readOnly = true)
    public FolderResponse getFolder(UUID folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Folder not found"));
        
        return buildFolderResponse(folder);
    }
    
    /**
     * List folders in a parent folder.
     * 
     * @param userId User ID
     * @param parentFolderId Parent folder ID (null for root)
     * @return List of folder responses
     */
    @Transactional(readOnly = true)
    public List<FolderResponse> listFolders(Long userId, UUID parentFolderId) {
        List<Folder> folders;
        
        if (parentFolderId == null) {
            folders = folderRepository.findByUserIdAndParentFolderIdIsNull(userId);
        } else {
            // Verify parent folder ownership
            verifyFolderOwnership(parentFolderId, userId);
            folders = folderRepository.findByUserIdAndParentFolderId(userId, parentFolderId);
        }
        
        return folders.stream()
                .map(this::buildFolderResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete a folder and all its contents.
     * 
     * @param folderId Folder ID
     * @param userId User ID
     */
    @Transactional
    public void deleteFolder(UUID folderId, Long userId) {
        log.info("Deleting folder - folderId: {}, userId: {}", folderId, userId);
        
        // 1. Get folder
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Folder not found"));
        
        // 2. Get all subfolders (recursive)
        List<Folder> subfolders = folderRepository.findAllSubfolders(folder.getFolderPath(), userId);
        
        // 3. Delete all files in this folder and subfolders
        deleteFilesInFolder(folder.getId(), userId);
        for (Folder subfolder : subfolders) {
            deleteFilesInFolder(subfolder.getId(), userId);
        }
        
        // 4. Delete subfolders
        folderRepository.deleteAll(subfolders);
        
        // 5. Delete folder
        folderRepository.delete(folder);
        
        log.info("Folder deleted successfully - folderId: {}, subfolders: {}", 
                 folderId, subfolders.size());
    }
    
    /**
     * Rename a folder.
     * 
     * @param folderId Folder ID
     * @param newName New folder name
     * @param userId User ID
     * @param username Username for audit
     * @return Updated folder response
     */
    @Transactional
    public FolderResponse renameFolder(UUID folderId, String newName, Long userId, String username) {
        log.info("Renaming folder - folderId: {}, newName: {}", folderId, newName);
        
        // 1. Get folder
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Folder not found"));
        
        // 2. Check if new name conflicts
        if (folderRepository.existsByUserIdAndParentFolderIdAndFolderName(
                userId, folder.getParentFolderId(), newName)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, 
                    "A folder with this name already exists in the same location");
        }
        
        // 3. Update folder name and path
        String oldPath = folder.getFolderPath();
        folder.setFolderName(newName);
        folder.setFolderPath(buildFolderPath(userId, folder.getParentFolderId(), newName));
        folder.setLastModifiedBy(username);
        
        // 4. Update paths of all subfolders
        List<Folder> subfolders = folderRepository.findAllSubfolders(oldPath, userId);
        for (Folder subfolder : subfolders) {
            String relativePath = subfolder.getFolderPath().substring(oldPath.length());
            subfolder.setFolderPath(folder.getFolderPath() + relativePath);
        }
        
        // 5. Save changes
        folderRepository.save(folder);
        if (!subfolders.isEmpty()) {
            folderRepository.saveAll(subfolders);
        }
        
        log.info("Folder renamed successfully - folderId: {}, newPath: {}", 
                 folderId, folder.getFolderPath());
        
        return buildFolderResponse(folder);
    }
    
    /**
     * Move a folder to a new parent.
     * 
     * @param folderId Folder ID to move
     * @param newParentId New parent folder ID (null for root)
     * @param userId User ID
     * @param username Username for audit
     * @return Updated folder response
     */
    @Transactional
    public FolderResponse moveFolder(UUID folderId, UUID newParentId, Long userId, String username) {
        log.info("Moving folder - folderId: {}, newParent: {}", folderId, newParentId);
        
        // 1. Get folder
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Folder not found"));
        
        // 2. Validate move (can't move to itself or its children)
        if (newParentId != null) {
            if (folderId.equals(newParentId)) {
                throw new BaseException(ErrorCode.BAD_REQUEST, 
                        "Cannot move folder to itself");
            }
            
            Folder newParent = folderRepository.findByIdAndUserId(newParentId, userId)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Target folder not found"));
            
            if (newParent.getFolderPath().startsWith(folder.getFolderPath())) {
                throw new BaseException(ErrorCode.BAD_REQUEST, 
                        "Cannot move folder to its own subfolder");
            }
        }
        
        // 3. Check for name conflict in new location
        if (folderRepository.existsByUserIdAndParentFolderIdAndFolderName(
                userId, newParentId, folder.getFolderName())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, 
                    "A folder with this name already exists in the target location");
        }
        
        // 4. Update folder path
        String oldPath = folder.getFolderPath();
        folder.setParentFolderId(newParentId);
        folder.setFolderPath(buildFolderPath(userId, newParentId, folder.getFolderName()));
        folder.setLastModifiedBy(username);
        
        // 5. Update paths of all subfolders
        List<Folder> subfolders = folderRepository.findAllSubfolders(oldPath, userId);
        for (Folder subfolder : subfolders) {
            String relativePath = subfolder.getFolderPath().substring(oldPath.length());
            subfolder.setFolderPath(folder.getFolderPath() + relativePath);
        }
        
        // 6. Save changes
        folderRepository.save(folder);
        if (!subfolders.isEmpty()) {
            folderRepository.saveAll(subfolders);
        }
        
        log.info("Folder moved successfully - folderId: {}, newPath: {}", 
                 folderId, folder.getFolderPath());
        
        return buildFolderResponse(folder);
    }
    
    /**
     * Build folder path from parent and name.
     */
    private String buildFolderPath(Long userId, UUID parentFolderId, String folderName) {
        if (parentFolderId == null) {
            return "/" + folderName;
        }
        
        Folder parent = folderRepository.findByIdAndUserId(parentFolderId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Parent folder not found"));
        
        return parent.getFolderPath() + "/" + folderName;
    }
    
    /**
     * Delete all files in a folder.
     */
    private void deleteFilesInFolder(UUID folderId, Long userId) {
        List<FileMetadata> files = fileMetadataRepository.findByFolderId(folderId);
        
        for (FileMetadata file : files) {
            // Delete from storage
            storageService.deleteFile(file.getStorageKey());
        }
        
        // Delete from database
        fileMetadataRepository.deleteAll(files);
        
        log.debug("Deleted {} files from folder {}", files.size(), folderId);
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
     * Build folder response DTO.
     */
    private FolderResponse buildFolderResponse(Folder folder) {
        long fileCount = fileMetadataRepository.countByFolderId(folder.getId());
        long subfolderCount = folderRepository.findByUserIdAndParentFolderId(
                folder.getUserId(), folder.getId()).size();
        
        return FolderResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getFolderName())
                .parentFolderId(folder.getParentFolderId())
                .folderPath(folder.getFolderPath())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .fileCount(fileCount)
                .subfolderCount(subfolderCount)
                .build();
    }
}
