package com.ziboto.backend.audit.service;

import com.ziboto.backend.audit.entity.AuditAction;
import com.ziboto.backend.audit.entity.AuditLog;
import com.ziboto.backend.audit.repository.AuditLogRepository;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Audit service implementation.
 * 
 * <p>Logs all security-critical operations and data access events:</p>
 * <ul>
 *   <li>Authentication events (login, logout)</li>
 *   <li>Data access (read, create, update, delete)</li>
 *   <li>File operations (upload, download, share)</li>
 *   <li>Administrative actions</li>
 * </ul>
 * 
 * <p>Audit logs are created asynchronously to avoid blocking application flow.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    
    /**
     * Log an audit event for a specific user.
     * 
     * @param userId user ID performing the action
     * @param entityType type of entity affected
     * @param entityId ID of entity affected
     * @param action action performed
     * @param details additional details (JSON or text)
     */
    @Override
    @Async
    @Transactional
    public void log(Long userId, String entityType, Long entityId, AuditAction action, String details) {
        try {
            User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
            
            // Extract request information if available
            String ipAddress = null;
            String userAgent = null;
            
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = extractClientIpAddress(request);
                userAgent = request.getHeader("User-Agent");
            }
            
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            
            auditLogRepository.save(auditLog);
            
            log.debug("Audit log created - user: {}, entity: {}, action: {}", userId, entityType, action);
            
        } catch (Exception e) {
            // Don't fail the operation if audit logging fails
            log.error("Failed to create audit log - user: {}, entity: {}, action: {}", 
                    userId, entityType, action, e);
        }
    }
    
    /**
     * Log an audit event using the current authenticated user.
     * 
     * @param entityType type of entity affected
     * @param entityId ID of entity affected
     * @param action action performed
     * @param details additional details
     */
    @Override
    @Async
    @Transactional
    public void log(String entityType, Long entityId, AuditAction action, String details) {
        try {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            
            if (authentication != null && authentication.isAuthenticated() 
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                String username = authentication.getName();
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    userId = user.getId();
                }
            }
            
            log(userId, entityType, entityId, action, details);
            
        } catch (Exception e) {
            log.error("Failed to create audit log - entity: {}, action: {}", entityType, action, e);
        }
    }
    
    /**
     * Extract client IP address from HTTP request.
     * Checks proxy headers first, then falls back to remote address.
     * 
     * @param request HTTP servlet request
     * @return client IP address
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress;
    }
}
