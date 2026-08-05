# 🚀 How to Run Ziboto Backend

## ⚡ Super Quick Start (30 seconds)

```bash
cd /home/rayan/Projects/ziboto/apps/backend
./RUN_ME.sh
```

Done! The script automatically:
- ✅ Checks/creates .env file
- ✅ Loads environment variables
- ✅ Verifies PostgreSQL is running
- ✅ Verifies Redis is running
- ✅ Starts the application

## Alternative Methods

### Method 1: Simple Script (Fastest)
```bash
./start.sh
```

### Method 2: With Environment Display
```bash
./run-with-env.sh
```

### Method 3: Manual (If scripts don't work)
```bash
# Load environment variables
source .env

# Run with JWT secret as argument
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.security.jwt.secret=${JWT_SECRET}"
```

### Method 4: One-liner
```bash
export JWT_SECRET=$(grep JWT_SECRET .env | cut -d= -f2) && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.security.jwt.secret=${JWT_SECRET}"
```

## ✅ Verify It's Running

Once you see "Started BackendApplication", test it:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

## 🧪 Test Authentication

### Register a User
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

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "test@example.com",
    "password": "SecurePass123!"
  }'
```

You should receive JWT tokens!

## 🔧 Troubleshooting

### Problem: "JWT secret is not configured"

**Solution:** Use one of the startup scripts (`./RUN_ME.sh` or `./start.sh`)

These scripts automatically pass the JWT_SECRET to Spring Boot.

### Problem: PostgreSQL or Redis not running

**Solution:**
```bash
# Start dependencies
docker-compose up -d postgres redis

# Verify
docker ps | grep -E '(postgres|redis)'
```

### Problem: Port 8080 already in use

**Solution:** Change port in `.env`:
```bash
echo "SERVER_PORT=8081" >> .env
```

## 📁 What Scripts Do

| Script | Purpose | When to Use |
|--------|---------|-------------|
| `RUN_ME.sh` | **Recommended** - Full checks + auto-start dependencies | First time or production |
| `start.sh` | Quick start with JWT secret | Daily development |
| `run-with-env.sh` | Start with env display | When debugging config |

## 🌐 Access Points

Once running, access:

- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health**: http://localhost:8080/actuator/health
- **API Docs**: http://localhost:8080/api-docs

## 🛑 Stop Application

Press `Ctrl+C` in the terminal where it's running

## 📚 Need More Help?

See:
- `QUICK_FIX.md` - Quick troubleshooting
- `docs/TROUBLESHOOTING.md` - Detailed troubleshooting
- `docs/QUICK_START_AUTH.md` - Complete tutorial
- `START_HERE.txt` - Overview

---

**TL;DR:** Just run `./RUN_ME.sh` 🎉
