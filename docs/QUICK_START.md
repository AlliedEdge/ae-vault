# Quick Start Guide - Login Flow

## Prerequisites

1. **Java 21** installed
2. **PostgreSQL** running (port 5432)
3. **Redis** running (port 6379)
4. **Maven** (included via wrapper)

## Setup Steps

### 1. Generate JWT Secret

```bash
# Generate a secure secret key
openssl rand -base64 64

# Copy the output and set as environment variable
export JWT_SECRET="<your-generated-secret>"
```

### 2. Configure Database

```bash
# Create database
psql -U postgres
CREATE DATABASE ziboto;
CREATE USER ziboto WITH PASSWORD 'ziboto';
GRANT ALL PRIVILEGES ON DATABASE ziboto TO ziboto;
\q
```

Or use Docker Compose:

```bash
cd /home/rayan/Projects/ziboto/apps/backend
docker-compose up -d postgres redis
```

### 3. Set Environment Variables

```bash
# Required
export JWT_SECRET="your-base64-encoded-secret"

# Optional (defaults shown)
export DATABASE_URL="jdbc:postgresql://localhost:5432/ziboto"
export DATABASE_USERNAME="ziboto"
export DATABASE_PASSWORD="ziboto"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export SPRING_PROFILES_ACTIVE="dev"
```

### 4. Run Database Migrations

```bash
cd /home/rayan/Projects/ziboto/apps/backend
./mvnw flyway:migrate
```

This will create:
- `users` table (with new `last_login_at` column)
- `refresh_tokens` table
- `audit_logs` table
- All necessary indexes

### 5. Build the Application

```bash
./mvnw clean package -DskipTests
```

### 6. Run the Application

```bash
./mvnw spring-boot:run
```

Or run the JAR:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**

### 7. Verify Setup

Check application health:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Testing the Login Flow

### 1. Create a Test User

First, register a user:

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
    "usernameOrEmail": "testuser",
    "password": "SecurePass123!"
  }'
```

Expected response (200 OK):
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "username": "testuser",
      "email": "test@example.com",
      "role": "USER",
      "status": "ACTIVE"
    }
  }
}
```

### 3. Use Access Token

```bash
# Save the access token from previous response
ACCESS_TOKEN="<your-access-token>"

# Make authenticated request
curl -X GET http://localhost:8080/api/v1/auth/verify \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 4. Run Complete Test Suite

Use the provided test script:

```bash
cd /home/rayan/Projects/ziboto/apps/backend
./test-login.sh
```

This will test:
- ✅ Valid login
- ✅ Invalid password
- ✅ Invalid username
- ✅ Rate limiting
- ✅ Token verification
- ✅ Token refresh
- ✅ Logout
- ✅ Token blacklist

## Verify Implementation

### Check Redis

```bash
# Connect to Redis
redis-cli

# View cached session
GET session:user:testuser

# View rate limit
GET rate_limit:login:testuser

# View failed attempts
GET failed_login:attempts:testuser

# View active sessions
HGETALL session:active:testuser

# Exit
exit
```

### Check Database

```bash
# Connect to PostgreSQL
psql -U ziboto -d ziboto

# Check last login time
SELECT id, username, email, last_login_at, created_at 
FROM users 
WHERE username = 'testuser';

# Check refresh tokens
SELECT id, user_id, ip_address, revoked, created_at, expires_at
FROM refresh_tokens 
WHERE user_id = (SELECT id FROM users WHERE username = 'testuser')
ORDER BY created_at DESC;

# Check audit logs
SELECT id, user_id, action, details, ip_address, created_at
FROM audit_logs 
WHERE action = 'LOGIN'
ORDER BY created_at DESC 
LIMIT 10;

# Exit
\q
```

## API Documentation

Once the application is running, access:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI Spec:** http://localhost:8080/api-docs

## Monitoring

Access actuator endpoints:

- **Health:** http://localhost:8080/actuator/health
- **Metrics:** http://localhost:8080/actuator/metrics
- **Prometheus:** http://localhost:8080/actuator/prometheus

## Common Issues & Solutions

### Issue: "JWT secret is not configured"

**Solution:**
```bash
export JWT_SECRET=$(openssl rand -base64 64)
```

### Issue: Redis connection refused

**Solution:**
```bash
# Start Redis
redis-server

# Or with Docker
docker-compose up -d redis

# Verify
redis-cli ping
# Should return: PONG
```

### Issue: Database connection failed

**Solution:**
```bash
# Check PostgreSQL is running
pg_isready -h localhost -p 5432

# Or with Docker
docker-compose up -d postgres

# Verify connection
psql -U ziboto -d ziboto -c "SELECT 1;"
```

### Issue: Port 8080 already in use

**Solution:**
```bash
# Use different port
export SERVER_PORT=8081
./mvnw spring-boot:run

# Or kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

### Issue: Flyway migration failed

**Solution:**
```bash
# Clean and retry
./mvnw flyway:clean flyway:migrate

# Or manually reset
psql -U ziboto -d ziboto
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
\q

./mvnw flyway:migrate
```

## Testing Rate Limiting

Test rate limiting (should fail after 5 attempts):

```bash
for i in {1..7}; do
  echo "Attempt $i:"
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "usernameOrEmail": "testuser",
      "password": "wrongpassword"
    }'
  echo -e "\n---"
  sleep 1
done
```

After 5 attempts, you should see:
```json
{
  "success": false,
  "message": "Too many login attempts. Please try again in X seconds.",
  "errorCode": "RATE_LIMIT_EXCEEDED"
}
```

## Testing Account Lockout

After 5 consecutive failed login attempts with correct username but wrong password:

```json
{
  "success": false,
  "message": "Account is locked due to multiple failed login attempts. Please try again in X seconds.",
  "errorCode": "ACCOUNT_LOCKED"
}
```

Unlock manually in Redis:
```bash
redis-cli DEL failed_login:lockout:testuser
redis-cli DEL failed_login:attempts:testuser
```

## Environment Profiles

### Development (dev)
```bash
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```
- Detailed logging
- SQL logging enabled
- H2 console available

### Production (prod)
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar target/backend-0.0.1-SNAPSHOT.jar
```
- Error logging only
- SQL logging disabled
- Security hardened

## Security Checklist

Before deploying to production:

- [ ] Generate strong JWT_SECRET (minimum 256 bits)
- [ ] Use strong database passwords
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS properly
- [ ] Set up Redis authentication
- [ ] Configure firewall rules
- [ ] Enable rate limiting
- [ ] Set up monitoring and alerting
- [ ] Configure log rotation
- [ ] Back up database regularly
- [ ] Review security headers

## Next Steps

1. **Add MFA/2FA** - Implement two-factor authentication
2. **Email Verification** - Verify email on new device login
3. **Password Reset** - Implement forgot password flow
4. **Social Login** - Add OAuth2 providers
5. **Session Management** - Add active sessions page
6. **Admin Dashboard** - Monitor auth events

## Support

For detailed documentation, see:
- [LOGIN_FLOW.md](./LOGIN_FLOW.md) - Complete flow documentation
- [LOGIN_FLOW_DIAGRAM.md](./LOGIN_FLOW_DIAGRAM.md) - Visual diagrams
- [AUTHENTICATION_SERVICE.md](./AUTHENTICATION_SERVICE.md) - Service details
- [REDIS_INTEGRATION.md](./REDIS_INTEGRATION.md) - Redis setup
- [SECURITY.md](./SECURITY.md) - Security best practices

## Troubleshooting Logs

View application logs:
```bash
tail -f logs/ziboto.log
```

Enable debug logging:
```bash
export LOG_LEVEL=DEBUG
./mvnw spring-boot:run
```

## Performance Tuning

### Redis Connection Pool
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### Database Connection Pool
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
```

### JVM Options
```bash
java -Xms512m -Xmx2g -XX:+UseG1GC \
  -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

**🎉 You're all set!**

The login flow is now fully implemented and ready for testing.

For questions or issues, check the documentation files in this directory.
