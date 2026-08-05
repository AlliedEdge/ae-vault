package com.ziboto.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token verification response")
public class VerifyTokenResponse {
    
    @NotNull
    @Schema(description = "Indicates if the token is valid", example = "true")
    private Boolean valid;
    
    @Schema(description = "Username associated with the token", example = "john_doe")
    private String username;
    
    @Schema(description = "User ID associated with the token", example = "12345")
    private Long userId;
    
    @Schema(description = "Token expiration timestamp", example = "2026-08-04T12:00:00")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Token issued at timestamp", example = "2026-08-04T10:00:00")
    private LocalDateTime issuedAt;
    
    @Schema(description = "Error message if token is invalid", example = "Token has expired")
    private String message;
}
