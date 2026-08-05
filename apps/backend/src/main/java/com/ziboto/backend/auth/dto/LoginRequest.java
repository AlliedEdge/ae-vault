package com.ziboto.backend.auth.dto;

import com.ziboto.backend.common.constant.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = ValidationMessages.USERNAME_OR_EMAIL_REQUIRED)
    private String usernameOrEmail;
    
    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    private String password;
}
