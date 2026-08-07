package com.ziboto.backend.file.repository;

import com.ziboto.backend.file.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Folder entity operations.
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {
    
    /**
     * Find all folders for a user in a specific parent folder.
     */
    List<Folder> findByUserIdAndParentFolderId(Long userId, UUID parentFolderId);
    
    /**
     * Find root folders (no parent) for a user.
     */
    List<Folder> findByUserIdAndParentFolderIdIsNull(Long userId);
    
    /**
     * Find folder by name and parent for a specific user.
     */
    Optional<Folder> findByUserIdAndParentFolderIdAndFolderName(
        Long userId, UUID parentFolderId, String folderName
    );
    
    /**
     * Check if a folder exists with given name under parent.
     */
    boolean existsByUserIdAndParentFolderIdAndFolderName(
        Long userId, UUID parentFolderId, String folderName
    );
    
    /**
     * Find folder by ID and user ID (for permission check).
     */
    Optional<Folder> findByIdAndUserId(UUID id, Long userId);
    
    /**
     * Get all subfolders recursively (for deletion).
     */
    @Query("SELECT f FROM Folder f WHERE f.folderPath LIKE CONCAT(:path, '%') AND f.userId = :userId")
    List<Folder> findAllSubfolders(@Param("path") String path, @Param("userId") Long userId);
    
    /**
     * Count folders for a user.
     */
    long countByUserId(Long userId);
}
