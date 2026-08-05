# Nginx Load Balancer Configuration

## Overview

Production-ready Nginx configuration for load balancing Ziboto backend with:
- **Load Balancing**: Least connection strategy across 3 Spring Boot instances
- **SSL/TLS**: HTTPS with modern security configuration
- **Rate Limiting**: Endpoint-specific rate limits (login, signup, API)
- **Health Checks**: Automatic backend health monitoring
- **Security Headers**: HSTS, CSP, X-Frame-Options, etc.
- **Compression**: Gzip for reduced bandwidth

## Quick Start

### 1. Generate SSL Certificates

For development (self-signed):
```bash
mkdir -p ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ssl/ziboto.key \
  -out ssl/ziboto.crt \
  -subj "/CN=api.ziboto.com"
```

For production (Let's Encrypt):
```bash
docker-compose -f docker-compose-nginx.yml up certbot
certbot certonly --webroot -w /var/www/certbot \
  -d api.ziboto.com \
  --email admin@ziboto.com \
  --agree-tos
```

### 2. Set Environment Variables

Create `.env` file:
```bash
DB_PASSWORD=your_secure_password
REDIS_PASSWORD=your_redis_password
JWT_SECRET=your_base64_encoded_secret
```

### 3. Start Services

```bash
# Start all services (Nginx + 3 Spring Boot instances + PostgreSQL + Redis)
docker-compose -f docker-compose-nginx.yml up -d

# Check service health
docker-compose -f docker-compose-nginx.yml ps

# View Nginx logs
docker logs -f ziboto-nginx

# View Spring Boot logs
docker logs -f spring-boot-1
```

### 4. Test Load Balancer

```bash
# Health check
curl http://localhost:8090/health

# Login endpoint (rate limited: 10/min)
curl -X POST https://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"user@example.com","password":"password"}'

# Check which backend handled request (see logs)
docker logs --tail=1 spring-boot-1
docker logs --tail=1 spring-boot-2
docker logs --tail=1 spring-boot-3
```

## Load Balancing Strategy

### Least Connections (least_conn)
Routes requests to the backend with fewest active connections.

**Benefits:**
- Fair distribution when requests have varying processing times
- Prevents overloading slow backends
- Better performance than round-robin for mixed workloads

**Alternative Strategies:**

1. **Round Robin (default)**:
   ```nginx
   upstream spring_boot_backend {
       server spring-boot-1:8081;
       server spring-boot-2:8082;
       server spring-boot-3:8083;
   }
   ```

2. **IP Hash (sticky sessions)**:
   ```nginx
   upstream spring_boot_backend {
       ip_hash;
       server spring-boot-1:8081;
       server spring-boot-2:8082;
       server spring-boot-3:8083;
   }
   ```

## Rate Limiting Configuration

| Endpoint | Rate Limit | Burst | Notes |
|----------|------------|-------|-------|
| `/api/v1/auth/login` | 10/min | 5 | Prevents brute force |
| `/api/v1/auth/register` | 5/hour | 2 | Prevents spam accounts |
| `/api/v1/auth/refresh` | 20/min | 10 | Token refresh |
| `/api/v1/**` (general) | 100/min | 30 | API rate limit |

### Customizing Rate Limits

Edit `nginx.conf`:
```nginx
# Increase login rate limit to 20/min
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=20r/m;

# Apply in location block
location /api/v1/auth/login {
    limit_req zone=login_limit burst=10 nodelay;
    # ...
}
```

## Health Checks

### Backend Health Check
Spring Boot actuator endpoint: `http://backend:8080/actuator/health`

### Nginx Health Check
Nginx status endpoint: `http://localhost:8090/health`

### Monitoring

```bash
# Nginx status
curl http://localhost:8090/nginx-status

# Spring Boot health (through load balancer)
curl http://localhost/actuator/health
```

## Security Configuration

### SSL/TLS Settings
- **Protocols**: TLS 1.2, TLS 1.3 only
- **Ciphers**: Mozilla Modern configuration
- **HSTS**: Enabled with 1-year max-age
- **OCSP Stapling**: Enabled

### Security Headers
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
```

## Scaling

### Adding Backend Instances

1. Add to `docker-compose-nginx.yml`:
```yaml
spring-boot-4:
  image: ziboto-backend:latest
  container_name: spring-boot-4
  environment:
    - SERVER_PORT=8084
  ports:
    - "8084:8084"
```

2. Add to `nginx.conf` upstream:
```nginx
upstream spring_boot_backend {
    least_conn;
    server spring-boot-1:8081;
    server spring-boot-2:8082;
    server spring-boot-3:8083;
    server spring-boot-4:8084;
}
```

3. Reload Nginx:
```bash
docker exec ziboto-nginx nginx -s reload
```

### Removing Backend Instances

1. Mark as down (no restart needed):
```nginx
server spring-boot-3:8083 down;
```

2. Or remove from upstream and reload.

## Troubleshooting

### 502 Bad Gateway
- Backend service is down or unreachable
- Check backend logs: `docker logs spring-boot-1`
- Check health: `curl http://localhost:8081/actuator/health`

### 429 Too Many Requests
- Rate limit exceeded
- Check rate limit configuration
- Client should implement exponential backoff

### SSL Certificate Errors
- Verify certificate files exist in `ssl/` directory
- Check certificate validity: `openssl x509 -in ssl/ziboto.crt -text -noout`
- For Let's Encrypt: ensure domain DNS points to server

### Connection Refused
- Verify backend services are running: `docker-compose ps`
- Check network connectivity: `docker network inspect ziboto-network`
- Verify ports are not blocked by firewall

## Performance Tuning

### Worker Processes
```nginx
worker_processes auto;  # Use all CPU cores
worker_connections 4096;  # Connections per worker
```

### Keepalive Connections
```nginx
keepalive 32;  # Persistent connections to backends
keepalive_requests 100;  # Requests per keepalive connection
```

### Buffer Sizes
```nginx
proxy_buffer_size 4k;
proxy_buffers 8 4k;
client_body_buffer_size 128k;
```

## Monitoring & Logging

### Log Locations
- **Access Log**: `/var/log/nginx/access.log`
- **Error Log**: `/var/log/nginx/error.log`

### Log Format
Includes timing information:
- `rt`: Request time
- `uct`: Upstream connect time
- `uht`: Upstream header time
- `urt`: Upstream response time

### Viewing Logs
```bash
# Real-time access log
docker exec ziboto-nginx tail -f /var/log/nginx/access.log

# Real-time error log
docker exec ziboto-nginx tail -f /var/log/nginx/error.log

# Filter by endpoint
docker exec ziboto-nginx grep "auth/login" /var/log/nginx/access.log
```

## Production Deployment Checklist

- [ ] Generate production SSL certificates (Let's Encrypt)
- [ ] Set strong passwords in `.env` file
- [ ] Configure DNS to point to load balancer
- [ ] Update `server_name` in `nginx.conf`
- [ ] Enable firewall rules (allow 80, 443)
- [ ] Configure log rotation
- [ ] Set up monitoring (Prometheus, Grafana)
- [ ] Configure automated SSL renewal
- [ ] Test failover (stop one backend instance)
- [ ] Load test with expected traffic
- [ ] Configure backup strategy
- [ ] Set up alerting (email, Slack, PagerDuty)

## Useful Commands

```bash
# Reload Nginx configuration (zero downtime)
docker exec ziboto-nginx nginx -s reload

# Test Nginx configuration
docker exec ziboto-nginx nginx -t

# View Nginx version and modules
docker exec ziboto-nginx nginx -V

# Stop all services
docker-compose -f docker-compose-nginx.yml down

# Stop and remove volumes
docker-compose -f docker-compose-nginx.yml down -v

# Scale Spring Boot instances
docker-compose -f docker-compose-nginx.yml up -d --scale spring-boot=5
```

## References

- [Nginx Load Balancing](https://docs.nginx.com/nginx/admin-guide/load-balancer/http-load-balancer/)
- [Nginx Rate Limiting](https://www.nginx.com/blog/rate-limiting-nginx/)
- [Mozilla SSL Configuration](https://ssl-config.mozilla.org/)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
