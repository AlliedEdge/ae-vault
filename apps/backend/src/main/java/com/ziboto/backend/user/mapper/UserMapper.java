package com.ziboto.backend.user.mapper;

import com.ziboto.backend.user.dto.ProfileUpdateRequest;
import com.ziboto.backend.user.dto.UpdateProfilePatchRequest;
import com.ziboto.backend.user.dto.UpdateProfileRequest;
import com.ziboto.backend.user.dto.UpdateProfileResponse;
import com.ziboto.backend.user.dto.UpdateUserRequest;
import com.ziboto.backend.user.dto.UserDto;
import com.ziboto.backend.user.dto.UserResponse;
import com.ziboto.backend.user.entity.User;
import org.mapstruct.*;

/**
 * MapStruct mapper for User entity and DTOs.
 * 
 * <p>Handles bidirectional mapping between User entity and various DTOs
 * including UserResponse, UserDto, and update requests.</p>
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    
    /**
     * Maps User entity to UserResponse DTO.
     * 
     * @param user user entity
     * @return user response DTO
     */
    UserResponse toResponse(User user);
    
    /**
     * Maps User entity to comprehensive UserDto.
     * 
     * @param user user entity
     * @return user DTO with all fields
     */
    UserDto toDto(User user);
    
    /**
     * Maps User entity to UpdateProfileResponse.
     * 
     * @param user user entity
     * @return update profile response DTO
     */
    UpdateProfileResponse toUpdateProfileResponse(User user);
    
    /**
     * Updates User entity from UpdateProfileRequest.
     * Only non-null fields from the request are applied to the entity.
     * 
     * @param request profile update request
     * @param user user entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "storageQuota", ignore = true)
    @Mapping(target = "storageUsed", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromProfileRequest(UpdateProfileRequest request, @MappingTarget User user);
    
    /**
     * Updates User entity from ProfileUpdateRequest.
     * Only non-null fields from the request are applied to the entity.
     * 
     * @param request profile update request
     * @param user user entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "storageQuota", ignore = true)
    @Mapping(target = "storageUsed", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromProfileUpdateRequest(ProfileUpdateRequest request, @MappingTarget User user);
    
    /**
     * Updates User entity from UpdateProfilePatchRequest.
     * Only non-null fields from the request are applied to the entity.
     * 
     * @param request profile patch request
     * @param user user entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "storageQuota", ignore = true)
    @Mapping(target = "storageUsed", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromPatchRequest(UpdateProfilePatchRequest request, @MappingTarget User user);
    
    /**
     * Updates User entity from UpdateUserRequest (admin operation).
     * Only non-null fields from the request are applied to the entity.
     * 
     * @param request user update request
     * @param user user entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "storageQuota", ignore = true)
    @Mapping(target = "storageUsed", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
