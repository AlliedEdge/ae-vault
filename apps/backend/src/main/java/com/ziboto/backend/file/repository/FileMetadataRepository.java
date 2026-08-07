package com.ziboto.backend.file.repository;

import com.ziboto.backend.file.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FileMetadata entity operations.
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    
    /**
     * Find file by ID and user ID (for permission check).
     */
    Optional<FileMetadata> findByIdAndUserId(UUID id, Long userId);
    
    /**
     * Find all files in a folder for a user.
     */
    Page<FileMetadata> findByUserIdAndFolderId(Long userId, UUID folderId, Pageable pageable);
    
    /**
     * Find all files in root (no folder) for a user.
     */
    Page<FileMetadata> findByUserIdAndFolderIdIsNull(Long userId, Pageable pageable);
    
    /**
     * Check for duplicate file by hash (deduplication).
     */
    Optional<FileMetadata> findByUserIdAndSha256Hash(Long userId, String sha256Hash);
    
    /**
     * Check if filename exists in the same folder for a user.
     * Used for duplicate filename detection when uploading.
     */
    boolean existsByUserIdAndFolderIdAndFileName(Long userId, UUID folderId, String fileName);
    
    /**
     * Search files by name.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.userId = :userId " +
           "AND (LOWER(f.fileName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<FileMetadata> searchByFileName(@Param("userId") Long userId, 
                                         @Param("query") String query, 
                                         Pageable pageable);
    
    /**
     * Get total storage used by user.
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.userId = :userId")
    Long getTotalStorageUsed(@Param("userId") Long userId);
    
    /**
     * Count files for a user.
     */
    long countByUserId(Long userId);
    
    /**
     * Count files in a folder.
     */
    long countByFolderId(UUID folderId);
    
    /**
     * Find all files in a folder (for deletion).
     */
    List<FileMetadata> findByFolderId(UUID folderId);
    
    /**
     * Increment download count.
     */
    @Modifying
    @Query("UPDATE FileMetadata f SET f.downloadCount = f.downloadCount + 1 WHERE f.id = :fileId")
    void incrementDownloadCount(@Param("fileId") UUID fileId);
    
    /**
     * Find files by MIME type.
     */
    Page<FileMetadata> findByUserIdAndMimeTypeStartingWith(
        Long userId, String mimeTypePrefix, Pageable pageable
    );
    
    /**
     * Get recent files for a user.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    Page<FileMetadata> findRecentFiles(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Calculate total storage by user ID (for StorageUsageService).
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.userId = :userId AND f.deletedAt IS NULL")
    Long calculateTotalStorageByUserId(@Param("userId") Long userId);
}
