# Database & Redis Connection Test Guide

## 🔍 Current Configuration

### Docker Compose (Actual Ports)
- **PostgreSQL**: Host port `5433` → Container port `5432`
- **Redis**: Host port `6380` → Container port `6379`

### Application.yml (Current - NEEDS UPDATE)
- **PostgreSQL**: `jdbc:postgresql://localhost:5432/ziboto` ❌ (Wrong port)
- **Redis**: `localhost:6379` ❌ (Wrong port)

## ⚠️ Port Mismatch Issue

Your application is trying to connect to:
- PostgreSQL on port **5432** (but Docker exposes it on **5433**)
- Redis on port **6379** (but Docker exposes it on **6380**)

## ✅ Solution: Update application.yml

You need to update the default ports to match your Docker configuration.

### Option 1: Update application.yml defaults (Recommended)

Update these lines in `application.yml`:

```yaml
datasource:
  url: ${DATABASE_URL:jdbc:postgresql://localhost:5433/ziboto}  # Changed 5432 → 5433
  
data:
  redis:
    port: ${REDIS_PORT:6380}  # Changed 6379 → 6380
```

### Option 2: Use environment variables

Create a `.env` file in the backend directory:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5433/ziboto
REDIS_PORT=6380
```

Then update your run configuration to load the .env file.

## 🧪 Testing Steps

### Step 1: Verify Docker Containers are Running

```bash
# Check all containers are up
docker ps

# You should see:
# - postgres:17
# - redis:7.4
# - pgadmin4
# - redisinsight
```

Expected output:
```
CONTAINER ID   IMAGE             STATUS         PORTS
abc123         postgres:17       Up 2 minutes   0.0.0.0:5433->5432/tcp
def456         redis:7.4         Up 2 minutes   0.0.0.0:6380->6379/tcp
ghi789         dpage/pgadmin4    Up 2 minutes   0.0.0.0:5050->80/tcp
jkl012         redis/redisinsight Up 2 minutes   0.0.0.0:5540->5540/tcp
```

### Step 2: Test PostgreSQL Connection (Direct)

```bash
# Test with psql command line
docker exec -it $(docker ps -qf "ancestor=postgres:17") psql -U ziboto -d ziboto

# Or from host machine (if psql installed)
psql -h localhost -p 5433 -U ziboto -d ziboto
# Password: ziboto123
```

Expected: Should connect successfully and show `ziboto=#` prompt.

**Quick test query:**
```sql
SELECT version();
\q  -- to quit
```

### Step 3: Test Redis Connection (Direct)

```bash
# Test with redis-cli from container
docker exec -it $(docker ps -qf "ancestor=redis:7.4") redis-cli

# Or from host machine (if redis-cli installed)
redis-cli -h localhost -p 6380
```

Expected: Should connect and show `localhost:6379>` prompt.

**Quick test commands:**
```bash
PING           # Should return PONG
SET test "hello"
GET test       # Should return "hello"
DEL test
exit
```

### Step 4: Test Using GUI Tools

#### pgAdmin (PostgreSQL)
1. Open browser: http://localhost:5050
2. Login:
   - Email: `admin@ziboto.com`
   - Password: `admin123`
3. Add Server:
   - Name: `Ziboto DB`
   - Host: `postgres` (container name) or `host.docker.internal`
   - Port: `5432` (internal container port)
   - Username: `ziboto`
   - Password: `ziboto123`
   - Database: `ziboto`

#### RedisInsight (Redis)
1. Open browser: http://localhost:5540
2. Add Database:
   - Host: `redis` or `host.docker.internal`
   - Port: `6379` (internal container port)
   - Name: `Ziboto Redis`

### Step 5: Test Spring Boot Connection

After updating the ports in application.yml, create this test:

```bash
# Navigate to backend directory
cd apps/backend

# Run the application
./mvnw spring-boot:run
```

**Watch for these log messages:**

✅ **Successful PostgreSQL Connection:**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

✅ **Successful Redis Connection:**
```
LettuceConnectionFactory - Initializing connection
```

✅ **Successful Flyway Migration:**
```
Flyway - Successfully validated X migrations
Flyway - Database: jdbc:postgresql://localhost:5433/ziboto
```

❌ **Failed Connection (If ports are wrong):**
```
Connection refused: localhost:5432
# or
Connection refused: localhost:6379
```

## 🔧 Create Connection Test Endpoints

I'll create test endpoints for you to verify connections.
