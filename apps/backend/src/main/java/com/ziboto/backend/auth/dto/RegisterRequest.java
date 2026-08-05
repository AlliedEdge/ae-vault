package com.ziboto.backend.auth.dto;

import com.ziboto.backend.common.constant.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = ValidationMessages.USERNAME_REQUIRED)
    @Size(min = 3, max = 50, message = ValidationMessages.USERNAME_SIZE)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = ValidationMessages.USERNAME_PATTERN)
    private String username;
    
    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @Email(message = ValidationMessages.EMAIL_INVALID)
    @Size(max = 100, message = ValidationMessages.EMAIL_SIZE)
    private String email;
    
    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @Size(min = 8, max = 100, message = ValidationMessages.PASSWORD_SIZE)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
            message = ValidationMessages.PASSWORD_STRENGTH)
    private String password;
    
    @Size(max = 100, message = ValidationMessages.FIRST_NAME_SIZE)
    private String firstName;
    
    @Size(max = 100, message = ValidationMessages.LAST_NAME_SIZE)
    private String lastName;
}
