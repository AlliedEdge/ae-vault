package com.ziboto.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user storage information.
 * 
 * <p>Provides storage usage statistics for the authenticated user.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageInfoResponse {
    
    private Long storageQuota;      // Total storage quota in bytes
    private Long storageUsed;       // Storage currently used in bytes
    private Long storageAvailable;  // Available storage in bytes
    private Double usagePercentage; // Storage usage percentage
    
    /**
     * Calculates storage info from quota and used values.
     * 
     * @param storageQuota total storage quota in bytes
     * @param storageUsed storage currently used in bytes
     * @return StorageInfoResponse with calculated values
     */
    public static StorageInfoResponse from(Long storageQuota, Long storageUsed) {
        Long quota = storageQuota != null ? storageQuota : 0L;
        Long used = storageUsed != null ? storageUsed : 0L;
        Long available = quota - used;
        Double percentage = quota > 0 ? (used * 100.0) / quota : 0.0;
        
        return StorageInfoResponse.builder()
                .storageQuota(quota)
                .storageUsed(used)
                .storageAvailable(available)
                .usagePercentage(Math.round(percentage * 100.0) / 100.0) // Round to 2 decimals
                .build();
    }
}
