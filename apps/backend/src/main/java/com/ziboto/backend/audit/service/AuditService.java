package com.ziboto.backend.audit.service;

import com.ziboto.backend.audit.entity.AuditAction;

public interface AuditService {
    
    void log(Long userId, String entityType, Long entityId, AuditAction action, String details);
    
    void log(String entityType, Long entityId, AuditAction action, String details);
}
