package com.ziboto.backend.auth.mapper;

import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.entity.RefreshToken;
import com.ziboto.backend.user.dto.UserResponse;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserRole;
import com.ziboto.backend.user.entity.UserStatus;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuthMapper {
    
    /**
     * Maps RegisterRequest to User entity.
     * Password should be encoded before mapping - this is done in the service layer.
     * Default values for role and status are set via @AfterMapping.
     */
    @Mapping(target = "password", ignore = true) // Password must be encoded separately
    @Mapping(target = "role", ignore = true) // Set in @AfterMapping
    @Mapping(target = "status", ignore = true) // Set in @AfterMapping
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "storageUsed", constant = "0L")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "storageQuota", ignore = true)
    User registerRequestToUser(RegisterRequest request);
    
    @AfterMapping
    default void setDefaultUserValues(@MappingTarget User user) {
        if (user.getRole() == null) {
            user.setRole(UserRole.ROLE_USER);
        }
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (user.getEmailVerified() == null) {
            user.setEmailVerified(false);
        }
    }
    
    /**
     * Maps User entity to UserResponse DTO.
     */
    UserResponse userToUserResponse(User user);
    
    /**
     * Creates AuthenticationResponse with tokens and user information.
     */
    @Mapping(target = "user", source = "user")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "tokenType", constant = "Bearer")
    AuthenticationResponse toAuthenticationResponse(
        User user,
        String accessToken,
        String refreshToken,
        Long expiresIn
    );
    
    /**
     * Alternative method to create AuthenticationResponse from UserResponse.
     */
    @Mapping(target = "user", source = "userResponse")
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "tokenType", constant = "Bearer")
    AuthenticationResponse toAuthenticationResponseFromDto(
        UserResponse userResponse,
        String accessToken,
        String refreshToken,
        Long expiresIn
    );
    
    /**
     * Maps RefreshToken entity to simplified DTO if needed in future.
     * Currently returns token hash representation.
     */
    default String refreshTokenToString(RefreshToken refreshToken) {
        return refreshToken != null ? refreshToken.getTokenHash() : null;
    }
}
