package com.ziboto.backend.file.repository;

import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.entity.FileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    Optional<FileMetadata> findByFileNameAndBucketId(String fileName, Long bucketId);
    
    Page<FileMetadata> findByBucketId(Long bucketId, Pageable pageable);
    
    Page<FileMetadata> findByBucketIdAndStatus(Long bucketId, FileStatus status, Pageable pageable);
    
    Page<FileMetadata> findByUploaderId(Long uploaderId, Pageable pageable);
    
    /**
     * Count total files for a specific user.
     * Optimized query that only counts records without loading entities.
     * 
     * @param uploaderId user ID
     * @return count of files uploaded by the user
     */
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.uploader.id = :uploaderId")
    Long countByUploaderId(@Param("uploaderId") Long uploaderId);
    
    /**
     * Count files with a specific status for a user.
     * 
     * @param uploaderId user ID
     * @param status file status
     * @return count of files with specified status
     */
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.uploader.id = :uploaderId AND f.status = :status")
    Long countByUploaderIdAndStatus(@Param("uploaderId") Long uploaderId, @Param("status") FileStatus status);
    
    /**
     * Calculate total storage used by a specific user.
     * Optimized query that sums file sizes without loading entities.
     * 
     * @param uploaderId user ID
     * @return total size in bytes, or 0 if no files
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.uploader.id = :uploaderId")
    Long calculateTotalStorageByUploaderId(@Param("uploaderId") Long uploaderId);
    
    /**
     * Calculate storage used by a user for files with a specific status.
     * 
     * @param uploaderId user ID
     * @param status file status
     * @return total size in bytes, or 0 if no files
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.uploader.id = :uploaderId AND f.status = :status")
    Long calculateStorageByUploaderIdAndStatus(@Param("uploaderId") Long uploaderId, @Param("status") FileStatus status);
}
