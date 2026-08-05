# Troubleshooting Guide

## Build and Compilation Issues

### Issue: "JWT secret must be configured"

**Error:**
```
ERROR - JWT secret is not configured! Set JWT_SECRET environment variable.
Caused by: java.lang.IllegalStateException: JWT secret must be configured
```

**Solution:**

1. **Create `.env` file** (already created at `/home/rayan/Projects/ziboto/apps/backend/.env`):
```bash
JWT_SECRET=KPcFiEqireVf3/dZdzwsipgWERdg0I/QGQva1ADPHEFRWFQbWY0cIDUrt7Zq3P0q/7Bi/JQGCosMxyl+rFSvlw==
```

2. **Run with environment variables loaded**:
```bash
# Option 1: Use the helper script
./run-with-env.sh

# Option 2: Export manually
export JWT_SECRET="KPcFiEqireVf3/dZdzwsipgWERdg0I/QGQva1ADPHEFRWFQbWY0cIDUrt7Zq3P0q/7Bi/JQGCosMxyl+rFSvlw=="
./mvnw spring-boot:run

# Option 3: Pass as Maven property
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.security.jwt.secret=KPcFiEqireVf3/dZdzwsipgWERdg0I/QGQva1ADPHEFRWFQbWY0cIDUrt7Zq3P0q/7Bi/JQGCosMxyl+rFSvlw=="
```

3. **Generate a new secret** (if needed):
```bash
openssl rand -base64 64
```

### Issue: Health Indicator Compilation Errors

**Error:**
```
package org.springframework.boot.actuate.health does not exist
cannot find symbol: class HealthIndicator
```

**Cause:** Spring Boot 4.x has different actuator API structure compared to 3.x

**Solution:** 
The custom health indicators have been temporarily removed. The application uses Spring Boot Actuator's built-in health endpoints:
- `/actuator/health` - Overall health status
- `/actuator/health/db` - Database health
- `/actuator/health/redis` - Redis health

These are automatically provided by Spring Boot Actuator.

## Running the Application

### Method 1: Using the Helper Script (Recommended)

```bash
cd /home/rayan/Projects/ziboto/apps/backend
./run-with-env.sh
```

This script automatically loads variables from `.env` file.

### Method 2: Manual Export

```bash
# Load .env variables
export $(grep -v '^#' .env | xargs)

# Run application
./mvnw spring-boot:run
```

### Method 3: IDE Configuration

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. Add Environment Variables from `.env` file
3. Or use EnvFile plugin

**VS Code:**
1. Create `.vscode/launch.json`
2. Add `envFile` property pointing to `.env`

## Testing the Application

### 1. Check if Application Started

```bash
# Check if running
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

### 2. Test Authentication Endpoints

**Register a User:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "test@example.com",
    "password": "SecurePass123!"
  }'
```

### 3. Check Logs

```bash
# Real-time logs
tail -f logs/ziboto.log

# Security logs
tail -f logs/ziboto-security.log

# Audit logs
tail -f logs/ziboto-audit.log
```

## Common Issues

### PostgreSQL Connection Refused

**Check if PostgreSQL is running:**
```bash
docker ps | grep postgres
```

**Start PostgreSQL:**
```bash
cd /home/rayan/Projects/ziboto/apps/backend
docker-compose up -d postgres
```

**Verify connection:**
```bash
./test-connections.sh
```

### Redis Connection Refused

**Check if Redis is running:**
```bash
docker ps | grep redis
```

**Start Redis:**
```bash
cd /home/rayan/Projects/ziboto/apps/backend
docker-compose up -d redis
```

**Test Redis connection:**
```bash
redis-cli -h localhost -p 6380 ping
# Expected: PONG
```

### Port 8080 Already in Use

**Find process using port 8080:**
```bash
lsof -i :8080
```

**Kill the process:**
```bash
kill -9 <PID>
```

**Or change the port:**
```bash
export SERVER_PORT=8081
./run-with-env.sh
```

## Quick Fixes

### Clean and Rebuild

```bash
# Clean Maven build
./mvnw clean

# Rebuild
./mvnw clean install -DskipTests

# Run
./run-with-env.sh
```

### Reset Database

```bash
# Stop containers
docker-compose down

# Remove volumes (WARNING: Deletes all data)
docker-compose down -v

# Start fresh
docker-compose up -d
```

### Clear Redis Cache

```bash
redis-cli -h localhost -p 6380
> FLUSHDB
> exit
```

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | ✅ Yes | - | Base64 JWT secret (64+ bytes) |
| `JWT_EXPIRATION` | No | 900000 | Access token expiry (15 min) |
| `JWT_REFRESH_EXPIRATION` | No | 604800000 | Refresh token expiry (7 days) |
| `DATABASE_URL` | No | jdbc:postgresql://localhost:5433/ziboto | PostgreSQL URL |
| `DATABASE_USERNAME` | No | ziboto | Database username |
| `DATABASE_PASSWORD` | No | ziboto123 | Database password |
| `REDIS_HOST` | No | localhost | Redis host |
| `REDIS_PORT` | No | 6380 | Redis port |
| `SERVER_PORT` | No | 8080 | Application port |

## Getting Help

1. **Check logs**: `tail -f logs/ziboto.log`
2. **Enable debug logging**: Set `LOG_LEVEL=DEBUG` in `.env`
3. **Test connections**: Run `./test-connections.sh`
4. **Check documentation**: See `docs/AUTHENTICATION_IMPLEMENTATION.md`
5. **Verify environment**: Run `env | grep -E '(JWT|DATABASE|REDIS)'`

---

**Last Updated:** August 5, 2026  
**Status:** Current with Spring Boot 4.1.0
