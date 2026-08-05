package com.ziboto.backend.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized logging configuration for production-grade logging.
 * 
 * <p>Configures:</p>
 * <ul>
 *   <li><b>Console Logging:</b> Formatted output for development</li>
 *   <li><b>File Logging:</b> Rolling file appender with size and time-based policies</li>
 *   <li><b>Security Event Logging:</b> Separate logger for security events</li>
 *   <li><b>Audit Logging:</b> Separate logger for audit trails</li>
 *   <li><b>Log Rotation:</b> Automatic rotation based on size and date</li>
 *   <li><b>Log Retention:</b> Configurable retention period</li>
 * </ul>
 * 
 * <h2>Log Files:</h2>
 * <ul>
 *   <li><b>ziboto.log:</b> Main application logs</li>
 *   <li><b>ziboto-security.log:</b> Security-related events</li>
 *   <li><b>ziboto-audit.log:</b> Audit trail logs</li>
 *   <li><b>ziboto-error.log:</b> Error and exception logs</li>
 * </ul>
 * 
 * <h2>Log Format:</h2>
 * <pre>
 * 2024-01-15 10:30:45.123 [thread-name] LEVEL logger-name - message
 * </pre>
 * 
 * <p>Security events include IP address, username, and request details.</p>
 */
@Slf4j
@Configuration
public class LoggingConfig {
    
    @Value("${logging.file.name:logs/ziboto.log}")
    private String logFileName;
    
    @Value("${logging.file.max-size:10MB}")
    private String maxFileSize;
    
    @Value("${logging.file.max-history:30}")
    private int maxHistory;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    /**
     * Initialize logging configuration after application startup.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Configures rolling file appenders</li>
     *   <li>Sets up log rotation policies</li>
     *   <li>Creates specialized loggers for security and audit</li>
     *   <li>Configures log formatting patterns</li>
     * </ul>
     */
    @PostConstruct
    public void initializeLogging() {
        log.info("Initializing centralized logging configuration");
        log.info("Active profile: {}", activeProfile);
        log.info("Log file: {}", logFileName);
        log.info("Max file size: {}, Max history: {} days", maxFileSize, maxHistory);
        
        try {
            // Get Logback context
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // Configure security logger
            configureSecurityLogger(loggerContext);
            
            // Configure audit logger
            configureAuditLogger(loggerContext);
            
            log.info("Logging configuration initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize logging configuration", e);
        }
    }
    
    /**
     * Configure security logger for security-related events.
     * 
     * <p>Security events include:</p>
     * <ul>
     *   <li>Login attempts (successful and failed)</li>
     *   <li>Authentication failures</li>
     *   <li>Authorization failures</li>
     *   <li>Rate limiting events</li>
     *   <li>Account lockouts</li>
     *   <li>Suspicious activity</li>
     * </ul>
     * 
     * @param loggerContext Logback logger context
     */
    private void configureSecurityLogger(LoggerContext loggerContext) {
        String securityLogFile = logFileName.replace(".log", "-security.log");
        
        Logger securityLogger = loggerContext.getLogger("com.ziboto.backend.security");
        
        // Create rolling file appender for security logs
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("SECURITY_FILE");
        fileAppender.setFile(securityLogFile);
        
        // Configure rolling policy
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(securityLogFile.replace(".log", "-%d{yyyy-MM-dd}-%i.log"));
        rollingPolicy.setMaxFileSize(FileSize.valueOf(maxFileSize));
        rollingPolicy.setMaxHistory(maxHistory);
        rollingPolicy.start();
        
        fileAppender.setRollingPolicy(rollingPolicy);
        
        // Configure encoder with detailed pattern for security events
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        
        fileAppender.setEncoder(encoder);
        fileAppender.start();
        
        securityLogger.addAppender(fileAppender);
        securityLogger.setAdditive(false); // Don't propagate to root logger
        
        log.debug("Security logger configured: {}", securityLogFile);
    }
    
    /**
     * Configure audit logger for audit trail events.
     * 
     * <p>Audit events include:</p>
     * <ul>
     *   <li>Data access (create, read, update, delete)</li>
     *   <li>Configuration changes</li>
     *   <li>User management actions</li>
     *   <li>Permission changes</li>
     *   <li>System configuration changes</li>
     * </ul>
     * 
     * @param loggerContext Logback logger context
     */
    private void configureAuditLogger(LoggerContext loggerContext) {
        String auditLogFile = logFileName.replace(".log", "-audit.log");
        
        Logger auditLogger = loggerContext.getLogger("com.ziboto.backend.audit");
        
        // Create rolling file appender for audit logs
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("AUDIT_FILE");
        fileAppender.setFile(auditLogFile);
        
        // Configure rolling policy
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(auditLogFile.replace(".log", "-%d{yyyy-MM-dd}-%i.log"));
        rollingPolicy.setMaxFileSize(FileSize.valueOf(maxFileSize));
        rollingPolicy.setMaxHistory(maxHistory * 2); // Keep audit logs longer
        rollingPolicy.start();
        
        fileAppender.setRollingPolicy(rollingPolicy);
        
        // Configure encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        
        fileAppender.setEncoder(encoder);
        fileAppender.start();
        
        auditLogger.addAppender(fileAppender);
        auditLogger.setAdditive(false); // Don't propagate to root logger
        
        log.debug("Audit logger configured: {}", auditLogFile);
    }
    
    /**
     * Get security logger instance.
     * Use this logger for all security-related events.
     * 
     * @return security logger
     */
    public static org.slf4j.Logger getSecurityLogger() {
        return LoggerFactory.getLogger("com.ziboto.backend.security");
    }
    
    /**
     * Get audit logger instance.
     * Use this logger for all audit trail events.
     * 
     * @return audit logger
     */
    public static org.slf4j.Logger getAuditLogger() {
        return LoggerFactory.getLogger("com.ziboto.backend.audit");
    }
    
    /**
     * Log security event with standard format.
     * 
     * @param event security event type
     * @param username username involved
     * @param ipAddress client IP address
     * @param details additional details
     */
    public static void logSecurityEvent(String event, String username, String ipAddress, String details) {
        org.slf4j.Logger securityLogger = getSecurityLogger();
        securityLogger.info("SECURITY_EVENT: {} - User: {}, IP: {}, Details: {}", 
                event, username, ipAddress, details);
    }
    
    /**
     * Log audit event with standard format.
     * 
     * @param action action performed
     * @param username username performing action
     * @param entityType entity type affected
     * @param entityId entity ID affected
     * @param details additional details
     */
    public static void logAuditEvent(String action, String username, String entityType, 
                                     Long entityId, String details) {
        org.slf4j.Logger auditLogger = getAuditLogger();
        auditLogger.info("AUDIT_EVENT: {} - User: {}, Entity: {}:{}, Details: {}", 
                action, username, entityType, entityId, details);
    }
}
