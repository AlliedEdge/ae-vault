package com.ziboto.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for comprehensive storage usage information.
 * 
 * <p>Provides complete storage statistics including quota, usage,
 * and counts of files and folders.</p>
 * 
 * <p>This DTO is cached in Redis for performance optimization.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsageResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Total storage quota allocated to the user in bytes.
     */
    private Long totalStorage;
    
    /**
     * Storage currently used by the user in bytes.
     */
    private Long usedStorage;
    
    /**
     * Available storage remaining in bytes.
     */
    private Long availableStorage;
    
    /**
     * Percentage of storage used (0.00 - 100.00).
     */
    private Double usedPercentage;
    
    /**
     * Total number of files owned by the user.
     */
    private Long totalFiles;
    
    /**
     * Total number of folders/buckets owned by the user.
     */
    private Long totalFolders;
    
    /**
     * Creates StorageUsageResponse from calculated values.
     * 
     * @param totalStorage total storage quota in bytes
     * @param usedStorage storage currently used in bytes
     * @param totalFiles total number of files
     * @param totalFolders total number of folders
     * @return StorageUsageResponse with all calculated values
     */
    public static StorageUsageResponse of(Long totalStorage, Long usedStorage, Long totalFiles, Long totalFolders) {
        Long total = totalStorage != null ? totalStorage : 0L;
        Long used = usedStorage != null ? usedStorage : 0L;
        Long available = total - used;
        Double percentage = total > 0 ? (used * 100.0) / total : 0.0;
        
        return StorageUsageResponse.builder()
                .totalStorage(total)
                .usedStorage(used)
                .availableStorage(available)
                .usedPercentage(Math.round(percentage * 100.0) / 100.0) // Round to 2 decimals
                .totalFiles(totalFiles != null ? totalFiles : 0L)
                .totalFolders(totalFolders != null ? totalFolders : 0L)
                .build();
    }
}
