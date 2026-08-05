package com.ziboto.backend.auth.service;

import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.mapper.AuthMapper;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.ConflictException;
import com.ziboto.backend.user.dto.UserResponse;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.mapper.UserMapper;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of RegistrationService for handling user registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Attempting to register user with username: {} and email: {}", 
                request.getUsername(), request.getEmail());
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists - {}", request.getEmail());
            throw new ConflictException(ErrorCode.USER_EMAIL_EXISTS);
        }
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username already exists - {}", request.getUsername());
            throw new ConflictException(ErrorCode.USER_USERNAME_EXISTS);
        }
        
        // Map request to user entity
        User user = authMapper.registerRequestToUser(request);
        
        // Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(hashedPassword);
        
        // Save user to database
        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {} and username: {}", 
                savedUser.getId(), savedUser.getUsername());
        
        // Map to response DTO
        return userMapper.toResponse(savedUser);
    }
}
