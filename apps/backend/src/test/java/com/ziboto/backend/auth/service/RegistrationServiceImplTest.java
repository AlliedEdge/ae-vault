package com.ziboto.backend.auth.service;

import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.mapper.AuthMapper;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.ConflictException;
import com.ziboto.backend.user.dto.UserResponse;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserRole;
import com.ziboto.backend.user.entity.UserStatus;
import com.ziboto.backend.user.mapper.UserMapper;
import com.ziboto.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private AuthMapper authMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private RegistrationServiceImpl registrationService;
    
    private RegisterRequest registerRequest;
    private User user;
    private UserResponse userResponse;
    
    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123")
                .firstName("Test")
                .lastName("User")
                .build();
        
        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .storageUsed(0L)
                .build();
        
        userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .storageUsed(0L)
                .build();
    }
    
    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(authMapper.registerRequestToUser(any(RegisterRequest.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);
        
        // Act
        UserResponse result = registrationService.register(registerRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(UserRole.ROLE_USER, result.getRole());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).existsByUsername("testuser");
        verify(authMapper).registerRequestToUser(registerRequest);
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }
    
    @Test
    void register_EmailAlreadyExists_ThrowsConflictException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        // Act & Assert
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> registrationService.register(registerRequest)
        );
        
        assertEquals(ErrorCode.USER_EMAIL_EXISTS, exception.getErrorCode());
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void register_UsernameAlreadyExists_ThrowsConflictException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        
        // Act & Assert
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> registrationService.register(registerRequest)
        );
        
        assertEquals(ErrorCode.USER_USERNAME_EXISTS, exception.getErrorCode());
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void register_PasswordIsEncoded() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(authMapper.registerRequestToUser(any(RegisterRequest.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);
        
        // Act
        registrationService.register(registerRequest);
        
        // Assert
        verify(passwordEncoder).encode("Password123");
        verify(user).setPassword("encodedPassword");
    }
}
