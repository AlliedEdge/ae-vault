# Quick Start: Authentication Setup

This guide will get you up and running with the Ziboto authentication system in 5 minutes.

## Prerequisites

- Docker & Docker Compose installed
- Java 17+ (for local development)
- Maven 3.8+ (for local development)

## Option 1: Docker Compose (Recommended)

### 1. Clone and Navigate
```bash
cd /home/rayan/Projects/ziboto/apps/backend
```

### 2. Create Environment File
```bash
cat > .env << EOF
# Database
DB_PASSWORD=$(openssl rand -base64 32)

# Redis
REDIS_PASSWORD=$(openssl rand -base64 32)

# JWT Secret (must be base64 encoded, 256+ bits)
JWT_SECRET=$(openssl rand -base64 64)

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
EOF
```

### 3. Generate SSL Certificates (Dev)
```bash
mkdir -p nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/ziboto.key \
  -out nginx/ssl/ziboto.crt \
  -subj "/CN=localhost"
```

### 4. Build Application
```bash
./mvnw clean package -DskipTests
docker build -t ziboto-backend:latest .
```

### 5. Start All Services
```bash
cd nginx
docker-compose -f docker-compose-nginx.yml up -d
```

### 6. Verify Everything is Running
```bash
# Check service status
docker-compose ps

# Check health
curl http://localhost/api/v1/health

# Expected response:
# {"status":"UP","timestamp":...}
```

## Option 2: Local Development

### 1. Start Dependencies Only
```bash
cd /home/rayan/Projects/ziboto/apps/backend
docker-compose up -d postgres redis
```

### 2. Set Environment Variables
```bash
export JWT_SECRET=$(openssl rand -base64 64)
export DATABASE_URL=jdbc:postgresql://localhost:5433/ziboto
export DATABASE_USERNAME=ziboto
export DATABASE_PASSWORD=ziboto123
export REDIS_HOST=localhost
export REDIS_PORT=6380
export SERVER_PORT=8080
```

### 3. Run Application
```bash
./mvnw spring-boot:run
```

### 4. Verify
```bash
curl http://localhost:8080/api/v1/health
```

## Test the Authentication

### 1. Register a User
```bash
curl -X POST http://localhost/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "userId": "...",
      "username": "testuser",
      "email": "test@example.com"
    }
  }
}
```

### 2. Login
```bash
curl -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "test@example.com",
    "password": "SecurePass123!"
  }'
```

### 3. Use Access Token
```bash
# Save the access token from login response
ACCESS_TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# Make authenticated request
curl -X GET http://localhost/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 4. Refresh Token
```bash
# Save the refresh token from login response
REFRESH_TOKEN="eyJhbGciOiJIUzUxMiJ9..."

curl -X POST http://localhost/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"
```

### 5. Logout
```bash
curl -X POST http://localhost/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## Verify Components

### Check Redis
```bash
docker exec -it ziboto-redis redis-cli

# Test commands:
AUTH your_redis_password
PING
KEYS *
INFO
```

### Check PostgreSQL
```bash
docker exec -it ziboto-postgres psql -U ziboto -d ziboto

# Test queries:
\dt                           -- List tables
SELECT * FROM users;          -- View users
SELECT * FROM refresh_tokens; -- View refresh tokens
SELECT * FROM audit_logs;     -- View audit logs
```

### Check Nginx
```bash
# View logs
docker logs -f ziboto-nginx

# Test load balancing - make multiple requests
for i in {1..10}; do
  curl -s http://localhost/api/v1/health | jq .
done

# Check which backend handled each request in logs
docker logs spring-boot-1 | grep "health"
docker logs spring-boot-2 | grep "health"
docker logs spring-boot-3 | grep "health"
```

## Access Swagger UI

Open in browser:
```
http://localhost/swagger-ui.html
```

## Common Issues

### "Connection refused" to PostgreSQL
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs ziboto-postgres

# Verify port
netstat -an | grep 5433
```

### "Connection refused" to Redis
```bash
# Check if Redis is running
docker ps | grep redis

# Check logs
docker logs ziboto-redis

# Test connection
redis-cli -h localhost -p 6380 ping
```

### "JWT secret must be configured"
```bash
# Ensure JWT_SECRET is set
echo $JWT_SECRET

# If empty, set it:
export JWT_SECRET=$(openssl rand -base64 64)

# Or add to .env file
echo "JWT_SECRET=$(openssl rand -base64 64)" >> .env
```

## Next Steps

1. **Read Full Documentation**: `docs/AUTHENTICATION_IMPLEMENTATION.md`
2. **Configure for Production**: Update SSL certificates, passwords
3. **Set Up Monitoring**: Configure Prometheus and Grafana
4. **Run Tests**: `./mvnw test`
5. **Load Testing**: See performance benchmarks in main docs

## Useful Commands

```bash
# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# Restart single service
docker-compose restart spring-boot-1

# Scale backend instances
docker-compose up -d --scale spring-boot=5

# Clean everything (including volumes)
docker-compose down -v
```

## Support

For more help, see:
- Full documentation: `docs/AUTHENTICATION_IMPLEMENTATION.md`
- Nginx setup: `nginx/README.md`
- Redis architecture: `docs/REDIS_ARCHITECTURE.md`
- Security guidelines: `docs/SECURITY.md`

---

**Quick Start Version:** 1.0.0  
**Last Updated:** August 5, 2026
