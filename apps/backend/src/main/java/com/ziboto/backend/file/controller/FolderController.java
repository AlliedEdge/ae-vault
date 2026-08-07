package com.ziboto.backend.file.controller;

import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.file.dto.FolderRequest;
import com.ziboto.backend.file.dto.FolderResponse;
import com.ziboto.backend.file.service.FolderService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for folder management operations.
 */
@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Folder Management", description = "APIs for folder creation and management")
public class FolderController {
    
    private final FolderService folderService;
    private final UserRepository userRepository;
    
    /**
     * Create a new folder.
     *
     * @param request Folder creation request
     * @param authentication Current user authentication
     * @return Created folder
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create folder", description = "Create a new folder")
    public ApiResponse<FolderResponse> createFolder(
            @Valid @RequestBody FolderRequest request,
            Authentication authentication) {
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = getUserId(authentication);
        
        log.info("Create folder request - user: {}, name: {}", 
                 userDetails.getUsername(), request.getFolderName());
        
        FolderResponse response = folderService.createFolder(request, userId, userDetails.getUsername());
        
        return ApiResponse.success("Folder created successfully", response);
    }
    
    /**
     * Get folder by ID.
     *
     * @param folderId Folder ID
     * @param authentication Current user authentication
     * @return Folder details
     */
    @GetMapping("/{folderId}")
    @Operation(summary = "Get folder", description = "Get folder details by ID")
    public ApiResponse<FolderResponse> getFolder(
            @PathVariable UUID folderId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        FolderResponse response = folderService.getFolder(folderId, userId);
        
        return ApiResponse.success(response);
    }
    
    /**
     * List folders in a parent folder.
     *
     * @param parentFolderId Parent folder ID (null for root)
     * @param authentication Current user authentication
     * @return List of folders
     */
    @GetMapping
    @Operation(summary = "List folders", description = "List folders in a parent folder (or root)")
    public ApiResponse<List<FolderResponse>> listFolders(
            @RequestParam(value = "parentFolderId", required = false) UUID parentFolderId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        List<FolderResponse> folders = folderService.listFolders(userId, parentFolderId);
        
        return ApiResponse.success(folders);
    }
    
    /**
     * Rename a folder.
     *
     * @param folderId Folder ID
     * @param newName New folder name
     * @param authentication Current user authentication
     * @return Updated folder
     */
    @PatchMapping("/{folderId}/rename")
    @Operation(summary = "Rename folder", description = "Rename an existing folder")
    public ApiResponse<FolderResponse> renameFolder(
            @PathVariable UUID folderId,
            @RequestParam("newName") String newName,
            Authentication authentication) {
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = getUserId(authentication);
        
        log.info("Rename folder request - folderId: {}, newName: {}", folderId, newName);
        
        FolderResponse response = folderService.renameFolder(
                folderId, newName, userId, userDetails.getUsername());
        
        return ApiResponse.success("Folder renamed successfully", response);
    }
    
    /**
     * Move a folder to a new parent.
     *
     * @param folderId Folder ID to move
     * @param newParentId New parent folder ID (null for root)
     * @param authentication Current user authentication
     * @return Updated folder
     */
    @PatchMapping("/{folderId}/move")
    @Operation(summary = "Move folder", description = "Move a folder to a new parent")
    public ApiResponse<FolderResponse> moveFolder(
            @PathVariable UUID folderId,
            @RequestParam(value = "newParentId", required = false) UUID newParentId,
            Authentication authentication) {
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = getUserId(authentication);
        
        log.info("Move folder request - folderId: {}, newParent: {}", folderId, newParentId);
        
        FolderResponse response = folderService.moveFolder(
                folderId, newParentId, userId, userDetails.getUsername());
        
        return ApiResponse.success("Folder moved successfully", response);
    }
    
    /**
     * Delete a folder and all its contents.
     *
     * @param folderId Folder ID
     * @param authentication Current user authentication
     * @return Success response
     */
    @DeleteMapping("/{folderId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete folder", description = "Delete a folder and all its contents")
    public ApiResponse<Void> deleteFolder(
            @PathVariable UUID folderId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Delete folder request - folderId: {}, userId: {}", folderId, userId);
        
        folderService.deleteFolder(folderId, userId);
        
        return ApiResponse.success("Folder deleted successfully", null);
    }
    
    /**
     * Extract user ID from authentication.
     * Fetches the user from database using username from JWT token.
     */
    private Long getUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with username: " + username
                ));
        
        return user.getId();
    }
}
