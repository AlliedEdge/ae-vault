# 🚀 Quick Connection Test

## ✅ Port Configuration Fixed

I've updated your `application.yml` to use the correct ports:
- **PostgreSQL**: `5433` (was 5432)
- **Redis**: `6380` (was 6379)

## 🧪 Step-by-Step Testing

### Step 1: Verify Docker Containers

```bash
# Navigate to docker directory
cd /home/rayan/Projects/ziboto/infra/docker

# Start containers (if not running)
docker-compose up -d

# Check status
docker-compose ps
```

**Expected output:**
```
NAME                 IMAGE              STATUS         PORTS
postgres             postgres:17        Up            0.0.0.0:5433->5432/tcp
redis                redis:7.4          Up            0.0.0.0:6380->6379/tcp
pgadmin              dpage/pgadmin4     Up            0.0.0.0:5050->80/tcp
redisinsight         redis/redisinsight Up            0.0.0.0:5540->5540/tcp
```

### Step 2: Test PostgreSQL (Direct)

```bash
# Option A: Using psql (if installed)
PGPASSWORD=ziboto123 psql -h localhost -p 5433 -U ziboto -d ziboto -c "SELECT version();"

# Option B: Using Docker exec
docker exec -it $(docker ps -qf "ancestor=postgres:17") psql -U ziboto -d ziboto -c "SELECT version();"
```

**Expected:** Should show PostgreSQL version

### Step 3: Test Redis (Direct)

```bash
# Option A: Using redis-cli (if installed)
redis-cli -h localhost -p 6380 PING

# Option B: Using Docker exec
docker exec -it $(docker ps -qf "ancestor=redis:7.4") redis-cli PING
```

**Expected:** Should return `PONG`

### Step 4: Run Automated Test Script

```bash
cd /home/rayan/Projects/ziboto/apps/backend

# Run test script
./test-connections.sh
```

This will check:
- ✅ Docker containers running
- ✅ PostgreSQL connection
- ✅ Redis connection  
- ✅ Configuration correctness
- ✅ Spring Boot endpoints (if running)

### Step 5: Start Spring Boot Application

```bash
cd /home/rayan/Projects/ziboto/apps/backend

# Option A: Using Maven wrapper
./mvnw spring-boot:run

# Option B: Using Maven (if installed)
mvn spring-boot:run
```

**Watch for these success messages:**

✅ **PostgreSQL Connection Success:**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

✅ **Redis Connection Success:**
```
LettuceConnectionFactory initialized
```

✅ **Flyway Migration Success:**
```
Flyway - Successfully validated X migrations
```

❌ **Connection Failed (Old Ports):**
```
Connection refused: localhost:5432  # Wrong PostgreSQL port
Connection refused: localhost:6379  # Wrong Redis port
```

### Step 6: Test Connection Endpoints

Once Spring Boot is running:

```bash
# Test all connections
curl http://localhost:8080/api/v1/test/connections | jq

# Test PostgreSQL only
curl http://localhost:8080/api/v1/test/postgres | jq

# Test Redis only
curl http://localhost:8080/api/v1/test/redis | jq
```

**Expected Response (All Healthy):**
```json
{
  "postgresql": {
    "connected": true,
    "status": "SUCCESS",
    "url": "jdbc:postgresql://localhost:5433/ziboto",
    "database": "ziboto",
    "username": "ziboto",
    "message": "PostgreSQL connection successful"
  },
  "redis": {
    "connected": true,
    "status": "SUCCESS",
    "ping": "PONG",
    "read_write_test": "PASSED",
    "message": "Redis connection successful"
  },
  "overall_status": "HEALTHY",
  "timestamp": "2024-01-20T15:30:00"
}
```

### Step 7: Access Management UIs

**pgAdmin (PostgreSQL Management):**
- URL: http://localhost:5050
- Login:
  - Email: `admin@ziboto.com`
  - Password: `admin123`
- Add Server:
  - Host: `host.docker.internal` (or `postgres`)
  - Port: `5432` (internal container port)
  - Username: `ziboto`
  - Password: `ziboto123`

**RedisInsight (Redis Management):**
- URL: http://localhost:5540
- Add Database:
  - Host: `host.docker.internal` (or `redis`)
  - Port: `6379` (internal container port)
  - Name: `Ziboto Redis`

## 🐛 Troubleshooting

### Issue: "Connection refused"

**Cause:** Wrong port in configuration

**Solution:**
```bash
# Check application.yml has correct ports
grep -A 2 "datasource:" apps/backend/src/main/resources/application.yml
# Should show: localhost:5433

grep -A 5 "redis:" apps/backend/src/main/resources/application.yml  
# Should show: port: 6380
```

### Issue: "Authentication failed"

**Cause:** Wrong credentials

**Solution:**
```bash
# Check docker-compose.yml
cat infra/docker/docker-compose.yml | grep -A 3 "POSTGRES"
# Should show: POSTGRES_PASSWORD: ziboto123

# Verify in application.yml
grep "password:" apps/backend/src/main/resources/application.yml
# Should show: password: ziboto123
```

### Issue: "Database 'ziboto' does not exist"

**Solution:**
```bash
# Connect to PostgreSQL and create database
docker exec -it $(docker ps -qf "ancestor=postgres:17") psql -U ziboto -d postgres -c "CREATE DATABASE ziboto;"
```

### Issue: Docker containers not running

**Solution:**
```bash
cd /home/rayan/Projects/ziboto/infra/docker
docker-compose down
docker-compose up -d
docker-compose ps
```

## 📊 Port Summary

| Service | Host Port | Container Port | URL |
|---------|-----------|----------------|-----|
| PostgreSQL | 5433 | 5432 | jdbc:postgresql://localhost:5433/ziboto |
| Redis | 6380 | 6379 | redis://localhost:6380 |
| pgAdmin | 5050 | 80 | http://localhost:5050 |
| RedisInsight | 5540 | 5540 | http://localhost:5540 |
| Backend API | 8080 | 8080 | http://localhost:8080 |

## ✅ Success Checklist

After running all tests, verify:

- [ ] Docker containers running (`docker ps` shows 4 containers)
- [ ] PostgreSQL connection successful (port 5433)
- [ ] Redis connection successful (port 6380)
- [ ] Spring Boot starts without errors
- [ ] Connection test endpoint returns HEALTHY
- [ ] pgAdmin can connect to database
- [ ] RedisInsight can connect to Redis
- [ ] Flyway migrations executed successfully
- [ ] Can create/read data in database
- [ ] Can set/get keys in Redis

## 🎯 Test Refresh Token Implementation

Once connections are verified:

```bash
# 1. Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123",
    "firstName": "Test",
    "lastName": "User"
  }'

# 2. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "Password123"
  }'

# 3. Save the refreshToken from response and test refresh
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "YOUR_REFRESH_TOKEN_HERE"}'

# 4. Check database for hashed token
docker exec -it $(docker ps -qf "ancestor=postgres:17") psql -U ziboto -d ziboto -c "SELECT id, LEFT(token_hash, 20) as hash_preview, user_id, created_at FROM refresh_tokens;"

# 5. Check Redis for session
redis-cli -h localhost -p 6380 KEYS "session:*"
redis-cli -h localhost -p 6380 GET "session:user:testuser"
```

## 📚 Additional Resources

- Full implementation: `REFRESH_TOKEN_IMPLEMENTATION.md`
- Testing guide: `REFRESH_TOKEN_TESTING.md`
- Connection guide: `CONNECTION_TEST_GUIDE.md`
- Frontend integration: `FRONTEND_INTEGRATION_GUIDE.md`

## 🆘 Need Help?

Run the diagnostic script:
```bash
./test-connections.sh
```

Check logs:
```bash
tail -f logs/ziboto.log
```

View Docker logs:
```bash
docker-compose -f infra/docker/docker-compose.yml logs -f postgres
docker-compose -f infra/docker/docker-compose.yml logs -f redis
```
