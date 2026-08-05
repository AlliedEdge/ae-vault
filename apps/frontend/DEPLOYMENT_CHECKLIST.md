# Production Deployment Checklist

## Pre-Deployment: Frontend Configuration

### Environment Variables
- [ ] Set `VITE_API_URL` to production Nginx load balancer URL
- [ ] Remove any development/debug flags
- [ ] Configure production error tracking (Sentry, etc.)
- [ ] Set up analytics tracking (Google Analytics, etc.)

```env
# Production .env
VITE_API_URL=https://api.ziboto.com/api/v1
VITE_ENVIRONMENT=production
```

### Build Optimization
- [ ] Run production build: `npm run build`
- [ ] Verify build output in `dist/` folder
- [ ] Check bundle size (should be < 500KB gzipped)
- [ ] Test build locally: `npm run preview`
- [ ] Verify all assets load correctly
- [ ] Check for console errors/warnings

### Security Headers
- [ ] Configure CSP (Content Security Policy)
- [ ] Enable HTTPS only mode
- [ ] Disable source maps in production
- [ ] Remove console.log statements or use conditional logging

## Pre-Deployment: Backend Configuration

### Spring Boot Application
- [ ] Configure JWT secret (256-bit minimum)
- [ ] Set access token expiry (15 minutes recommended)
- [ ] Set refresh token expiry (7 days recommended)
- [ ] Configure CORS for production frontend URL
- [ ] Set up database connection pooling
- [ ] Configure Redis connection (host, port, password)

```yaml
# application-prod.yml
spring:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiry: 900000    # 15 minutes (ms)
      refresh-token-expiry: 604800000 # 7 days (ms)
  
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      password: ${REDIS_PASSWORD}
```

### Database Setup (PostgreSQL)
- [ ] Run database migrations (Flyway/Liquibase)
- [ ] Create indexes on frequently queried columns
  - [ ] `users.email` (UNIQUE)
  - [ ] `refresh_tokens.user_id`
  - [ ] `refresh_tokens.expires_at`
  - [ ] `audit_logs.user_id`
  - [ ] `audit_logs.timestamp`
- [ ] Set up database backups (automated)
- [ ] Configure read replicas (if needed)
- [ ] Test connection from application

### Redis Setup
- [ ] Deploy Redis (standalone or cluster)
- [ ] Enable persistence (RDB + AOF)
- [ ] Configure maxmemory policy (allkeys-lru)
- [ ] Set up Redis Sentinel for high availability
- [ ] Test connection from application

### Rate Limiting Configuration
- [ ] Implement rate limiting in Spring Boot
- [ ] Configure Redis for rate limit storage
- [ ] Set limits:
  - [ ] Login: 5 attempts per 15 minutes per IP
  - [ ] Register: 3 attempts per hour per IP
  - [ ] Password reset: 3 requests per hour per IP
  - [ ] Token refresh: 10 requests per minute per user
  - [ ] API endpoints: 100 requests per minute per user

### Token Revocation
- [ ] Implement Redis token blacklist
- [ ] Add token to blacklist on logout
- [ ] Check blacklist on each authenticated request
- [ ] Set TTL on blacklisted tokens (match access token expiry)

### Audit Logging
- [ ] Log all authentication events to PostgreSQL
- [ ] Log login (success/failure)
- [ ] Log logout
- [ ] Log token refresh
- [ ] Log password changes
- [ ] Log suspicious activities

## Nginx Load Balancer Setup

### Installation
- [ ] Install Nginx on load balancer server
- [ ] Configure systemd service for auto-start
- [ ] Open required ports (80, 443)

### Backend Upstream Configuration
```nginx
upstream ziboto_backend {
    least_conn;  # or ip_hash for sticky sessions
    
    server backend1.internal:8080 max_fails=3 fail_timeout=30s;
    server backend2.internal:8080 max_fails=3 fail_timeout=30s;
    server backend3.internal:8080 max_fails=3 fail_timeout=30s;
    
    # Health check (Nginx Plus) or use separate monitoring
    # health_check interval=10s fails=3 passes=2;
}
```

### SSL/TLS Configuration
- [ ] Obtain SSL certificate (Let's Encrypt or commercial)
- [ ] Install certificate and private key
- [ ] Configure SSL in Nginx
- [ ] Enable HTTP/2
- [ ] Configure cipher suites (strong only)
- [ ] Enable HSTS (HTTP Strict Transport Security)

```nginx
ssl_certificate /etc/nginx/ssl/ziboto.com.crt;
ssl_certificate_key /etc/nginx/ssl/ziboto.com.key;
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers HIGH:!aNULL:!MD5;
ssl_prefer_server_ciphers on;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

### CORS Configuration
```nginx
# In server block
add_header 'Access-Control-Allow-Origin' 'https://ziboto.com' always;
add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
add_header 'Access-Control-Allow-Credentials' 'false' always;
add_header 'Access-Control-Max-Age' 1728000 always;

# Handle preflight OPTIONS requests
if ($request_method = 'OPTIONS') {
    return 204;
}
```

### Security Headers
```nginx
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
```

### Proxy Configuration
```nginx
location /api/v1/ {
    proxy_pass http://ziboto_backend;
    proxy_http_version 1.1;
    
    # Headers
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # Timeouts
    proxy_connect_timeout 30s;
    proxy_send_timeout 30s;
    proxy_read_timeout 30s;
    
    # Buffering
    proxy_buffering on;
    proxy_buffer_size 4k;
    proxy_buffers 8 4k;
    
    # Error handling
    proxy_next_upstream error timeout http_502 http_503 http_504;
    proxy_next_upstream_tries 3;
}
```

### Health Checks
- [ ] Implement health check endpoint in Spring Boot: `/actuator/health`
- [ ] Configure Nginx to check backend health
- [ ] Set up monitoring alerts for unhealthy backends

## Deployment: Multiple Spring Boot Instances

### Instance 1, 2, 3...
- [ ] Build application JAR: `./mvnw clean package -Pprod`
- [ ] Deploy JAR to each server
- [ ] Create systemd service file for auto-start
- [ ] Configure application properties for production
- [ ] Set JVM options (heap size, GC settings)
- [ ] Start application
- [ ] Verify logs for startup errors

```bash
# Systemd service file: /etc/systemd/system/ziboto-backend.service
[Unit]
Description=Ziboto Backend Service
After=network.target

[Service]
Type=simple
User=ziboto
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/ziboto/backend.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Verification
- [ ] Each instance starts successfully
- [ ] Health endpoint responds: `curl http://localhost:8080/actuator/health`
- [ ] JWT validation works on each instance
- [ ] Database connections established
- [ ] Redis connections established

## Testing: Integration Tests

### Authentication Flow
- [ ] Register new user through load balancer
- [ ] Login through load balancer
- [ ] Verify JWT token received
- [ ] Make authenticated request to each backend instance
- [ ] Verify all instances accept the same JWT
- [ ] Test token refresh through load balancer
- [ ] Test logout through load balancer
- [ ] Verify tokens revoked (in Redis blacklist)

### Load Balancing
- [ ] Send 100 requests and verify distribution across instances
- [ ] Stop one instance and verify failover works
- [ ] Restart instance and verify it rejoins pool
- [ ] Test with expired access token (should auto-refresh)
- [ ] Test with invalid refresh token (should logout)

### Rate Limiting
- [ ] Test login rate limit (5 attempts per 15 min)
- [ ] Verify 429 response with proper error message
- [ ] Test register rate limit (3 attempts per hour)
- [ ] Test API rate limit (100 requests per minute)
- [ ] Verify rate limits are shared across instances (Redis)

### Token Revocation
- [ ] Login on Device A
- [ ] Logout on Device A
- [ ] Verify subsequent requests with old token fail
- [ ] Verify token is in Redis blacklist
- [ ] Wait for TTL and verify token removed from blacklist

### Security
- [ ] Verify HTTPS enforced (HTTP redirects to HTTPS)
- [ ] Test CORS with allowed origin (should work)
- [ ] Test CORS with different origin (should fail)
- [ ] Verify security headers present in responses
- [ ] Test SQL injection attempts (should be blocked)
- [ ] Test XSS attempts (should be blocked)

### Performance
- [ ] Run load test: 1000 concurrent users
- [ ] Measure average response time (should be < 200ms)
- [ ] Measure 99th percentile response time (should be < 1s)
- [ ] Verify no memory leaks (monitor for 24 hours)
- [ ] Verify Redis hit rate (should be > 90%)
- [ ] Verify database connection pool not exhausted

## Monitoring & Alerting

### Application Monitoring
- [ ] Set up Spring Boot Actuator metrics
- [ ] Expose metrics endpoint: `/actuator/metrics`
- [ ] Configure Prometheus scraping (if using)
- [ ] Create Grafana dashboards

### Metrics to Monitor
- [ ] Request rate (requests per second)
- [ ] Error rate (4xx, 5xx errors)
- [ ] Response time (average, p50, p95, p99)
- [ ] JWT validation time
- [ ] Token refresh rate
- [ ] Database connection pool usage
- [ ] Redis connection pool usage
- [ ] JVM heap usage
- [ ] CPU usage
- [ ] Disk usage

### Alerts to Configure
- [ ] Backend instance down
- [ ] Error rate > 5%
- [ ] Response time > 1 second (p99)
- [ ] Database connection pool > 90% used
- [ ] Redis connection failures
- [ ] High failed login attempts (potential attack)
- [ ] JWT validation failures spike
- [ ] Disk usage > 85%

### Logging
- [ ] Centralized logging (ELK stack, CloudWatch, etc.)
- [ ] Log rotation configured
- [ ] Log retention policy (30 days recommended)
- [ ] Structured logging (JSON format)
- [ ] Log levels configured (INFO for prod, DEBUG for staging)

## Backup & Disaster Recovery

### Database Backups
- [ ] Automated daily backups
- [ ] Test restore procedure
- [ ] Off-site backup storage
- [ ] Point-in-time recovery enabled
- [ ] Backup retention: 30 days

### Redis Backups
- [ ] RDB snapshots every 6 hours
- [ ] AOF enabled for durability
- [ ] Backup Redis dump files
- [ ] Test Redis restore procedure

### Application Backups
- [ ] Version control (Git) for source code
- [ ] JAR artifacts in artifact repository
- [ ] Configuration files backed up
- [ ] SSL certificates backed up (encrypted)

### Disaster Recovery Plan
- [ ] Document recovery steps
- [ ] Assign responsibilities
- [ ] Recovery Time Objective (RTO): < 1 hour
- [ ] Recovery Point Objective (RPO): < 15 minutes
- [ ] Test recovery procedure quarterly

## Documentation

- [ ] Update API documentation
- [ ] Document deployment process
- [ ] Document rollback procedure
- [ ] Document monitoring dashboard usage
- [ ] Document common troubleshooting steps
- [ ] Update runbooks for operations team

## Post-Deployment

### Smoke Tests
- [ ] Access frontend URL: https://ziboto.com
- [ ] Register new user
- [ ] Login with new user
- [ ] Navigate to protected pages
- [ ] Logout
- [ ] Verify email verification flow (if applicable)
- [ ] Test forgot password flow

### Monitoring (First 24 Hours)
- [ ] Monitor error logs
- [ ] Monitor application metrics
- [ ] Monitor database performance
- [ ] Monitor Redis performance
- [ ] Monitor user login success rate
- [ ] Check for any security alerts

### Performance Tuning
- [ ] Analyze slow queries (database)
- [ ] Optimize Redis cache settings
- [ ] Tune JVM garbage collection
- [ ] Adjust connection pool sizes
- [ ] Optimize Nginx worker processes

### User Communication
- [ ] Announce launch to users
- [ ] Provide support contact information
- [ ] Monitor user feedback
- [ ] Prepare for support requests

## Rollback Plan

### If Critical Issues Detected
1. [ ] Stop sending traffic to new version (Nginx upstream)
2. [ ] Route traffic to previous version
3. [ ] Investigate issues in logs
4. [ ] Fix issues in development environment
5. [ ] Re-deploy with fixes
6. [ ] Test thoroughly
7. [ ] Re-enable traffic to new version

### Database Rollback
1. [ ] Stop application instances
2. [ ] Restore database from backup
3. [ ] Roll back migrations (if needed)
4. [ ] Restart application instances with previous version
5. [ ] Verify data integrity

## Security Audit

- [ ] Run security scan (OWASP ZAP, Burp Suite)
- [ ] Check for known vulnerabilities in dependencies
- [ ] Verify password storage (BCrypt with rounds=10)
- [ ] Verify JWT secret strength (256-bit minimum)
- [ ] Test for common attacks:
  - [ ] SQL Injection
  - [ ] XSS
  - [ ] CSRF
  - [ ] Brute force
  - [ ] Session fixation
  - [ ] Clickjacking
- [ ] Review CORS configuration
- [ ] Review CSP configuration
- [ ] Enable security headers
- [ ] Disable unnecessary HTTP methods

## Compliance (If Applicable)

- [ ] GDPR compliance (data privacy)
- [ ] CCPA compliance (California residents)
- [ ] PCI DSS compliance (if handling payments)
- [ ] HIPAA compliance (if handling health data)
- [ ] SOC 2 compliance (if B2B SaaS)

## Final Checks

- [ ] All items in this checklist completed
- [ ] Stakeholders notified of deployment
- [ ] Support team briefed on new features
- [ ] Documentation updated and accessible
- [ ] Monitoring dashboards accessible to team
- [ ] On-call rotation established
- [ ] Incident response plan in place

---

## Quick Reference: Production URLs

- **Frontend**: https://ziboto.com
- **API (via Nginx)**: https://api.ziboto.com/api/v1
- **Health Check**: https://api.ziboto.com/actuator/health
- **Metrics**: https://api.ziboto.com/actuator/metrics (internal only)
- **Monitoring Dashboard**: [Your Grafana URL]
- **Logs**: [Your ELK/CloudWatch URL]

## Emergency Contacts

- **DevOps Lead**: [Name] - [Phone] - [Email]
- **Backend Lead**: [Name] - [Phone] - [Email]
- **Frontend Lead**: [Name] - [Phone] - [Email]
- **DBA**: [Name] - [Phone] - [Email]
- **Security Officer**: [Name] - [Phone] - [Email]

---

**Deployment Date**: _______________
**Deployed By**: _______________
**Version**: _______________
**Sign-off**: _______________
