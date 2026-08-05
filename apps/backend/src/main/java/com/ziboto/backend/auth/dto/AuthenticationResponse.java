package com.ziboto.backend.auth.dto;

import com.ziboto.backend.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response containing tokens and user information")
public class AuthenticationResponse {
    
    @NotBlank
    @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @NotBlank
    @Schema(description = "Refresh token for obtaining new access tokens", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String refreshToken;
    
    @NotBlank
    @Builder.Default
    @Schema(description = "Type of token", example = "Bearer", defaultValue = "Bearer")
    private String tokenType = "Bearer";
    
    @NotNull
    @Positive
    @Schema(description = "Access token expiration time in seconds", example = "3600")
    private Long expiresIn;
    
    @NotNull
    @Schema(description = "Authenticated user information")
    private UserResponse user;
}
