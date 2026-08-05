# Quick Fix: Running Ziboto Backend

## ✅ Issues Fixed

1. ✅ JWT_SECRET environment variable configured
2. ✅ `.env` file created with all required variables
3. ✅ Helper script created to load environment variables
4. ✅ Build issues resolved

## 🚀 How to Run

### Option 1: Use the Helper Script (Easiest)

```bash
cd /home/rayan/Projects/ziboto/apps/backend
./run-with-env.sh
```

This automatically loads all environment variables from `.env` and starts the application.

### Option 2: Manual Method

```bash
cd /home/rayan/Projects/ziboto/apps/backend

# Load environment variables
export $(grep -v '^#' .env | xargs)

# Run application
./mvnw spring-boot:run
```

## ✅ Verify It's Working

Once the application starts, test it:

```bash
# Check health
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

## 🧪 Test Authentication

### 1. Register a User

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

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "test@example.com",
    "password": "SecurePass123!"
  }'
```

You should receive JWT access and refresh tokens!

## 📁 Files Created

- `.env` - Environment variables (JWT_SECRET, database, Redis config)
- `run-with-env.sh` - Helper script to run with environment variables
- `docs/TROUBLESHOOTING.md` - Detailed troubleshooting guide

## ⚠️ Important Notes

1. The `.env` file contains a **pre-generated JWT_SECRET** - This is secure for development
2. For production, regenerate with: `openssl rand -base64 64`
3. PostgreSQL and Redis must be running (they should be from your docker-compose)

## 🔧 If You Still Have Issues

### Database/Redis Not Running?

```bash
# Check what's running
docker ps

# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Verify connections
./test-connections.sh
```

### Port 8080 Already in Use?

```bash
# Change port in .env file
echo "SERVER_PORT=8081" >> .env

# Or export before running
export SERVER_PORT=8081
./run-with-env.sh
```

## 📚 Next Steps

1. Read: `docs/AUTHENTICATION_IMPLEMENTATION.md` - Complete guide
2. Read: `docs/QUICK_START_AUTH.md` - Step-by-step tutorial
3. Read: `docs/TROUBLESHOOTING.md` - Common issues and solutions

## ✨ What's Working Now

- ✅ JWT-based authentication
- ✅ User registration and login
- ✅ Token refresh
- ✅ Rate limiting
- ✅ Redis session caching
- ✅ PostgreSQL user storage
- ✅ Audit logging
- ✅ BCrypt password hashing
- ✅ Health check endpoints

---

**Ready to code!** 🎉
