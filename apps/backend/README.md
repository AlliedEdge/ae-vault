# Ziboto Backend

Cloud-native distributed object storage platform built with Spring Boot 3 and Java 21.

## 🔒 Security Features

This application implements **production-grade security** including:
- ✅ JWT-based authentication with token rotation
- ✅ Redis-based rate limiting (login, signup, API, token refresh)
- ✅ Automatic account locking after failed login attempts
- ✅ Comprehensive security headers (CSP, HSTS, X-Frame-Options, etc.)
- ✅ CORS protection with configurable origins
- ✅ Request validation with 100+ standardized messages
- ✅ Global exception handling with security logging
- ✅ Audit logging for all security events
- ✅ Token blacklisting for logout and revocation
- ✅ BCrypt password hashing

📖 **[View Complete Security Documentation →](docs/SECURITY.md)**

## Tech Stack

- **Java 21**
- **Spring Boot 3** (Web, Data JPA, Security, Validation, Actuator)
- **Spring Security 6** with JWT Authentication
- **PostgreSQL** - Primary database
- **Redis** - Caching layer
- **Flyway** - Database migrations
- **Maven** - Build tool
- **Lombok** - Reduce boilerplate
- **MapStruct** - Object mapping
- **OpenAPI (Swagger)** - API documentation
- **Docker** - Containerization

## Architecture

The project follows **Package-by-Feature** with **Layered Architecture**:

```
com.ziboto.backend
├── auth/              # Authentication & Authorization
├── user/              # User Management
├── storage/           # Bucket Management
├── file/              # File Management
├── audit/             # Audit Logging
├── common/            # Shared DTOs, entities, constants
├── config/            # Configuration classes
├── security/          # Security infrastructure (JWT, filters)
├── exception/         # Exception handling
└── cache/             # Redis cache configuration
```

Each feature module contains:
- `controller` - REST API endpoints
- `service` - Business logic
- `repository` - Data access
- `entity` - JPA entities
- `dto` - Data transfer objects
- `mapper` - MapStruct mappers
- `validator` - Custom validators (if needed)

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+
- Docker & Docker Compose (optional)

## Getting Started

### 1. Clone the repository

```bash
cd apps/backend
```

### 2. Start dependencies with Docker Compose

```bash
docker-compose up -d
```

This starts PostgreSQL and Redis containers.

### 3. Configure environment variables

Copy `.env.example` to `.env` and update values:

```bash
cp .env.example .env
```

**Important:** Generate a secure JWT secret (minimum 256 bits):

```bash
openssl rand -base64 32
```

### 4. Build the project

```bash
./mvnw clean install
```

### 5. Run the application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

Once the application is running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs

## Database Migrations

Flyway handles database migrations automatically on startup.

Migration files are located in: `src/main/resources/db/migration/`

To run migrations manually:

```bash
./mvnw flyway:migrate
```

## Project Structure

```
src/main/java/com/ziboto/backend/
├── auth/
│   ├── controller/
│   ├── service/
│   └── dto/
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
├── storage/
│   ├── service/
│   ├── repository/
│   └── entity/
├── file/
│   ├── service/
│   ├── repository/
│   └── entity/
├── audit/
│   ├── service/
│   ├── repository/
│   └── entity/
├── common/
│   ├── dto/           # ApiResponse, PageResponse
│   ├── entity/        # BaseEntity
│   └── constant/      # ErrorCode
├── config/            # Application configurations
│   └── properties/    # Configuration properties
├── security/          # JWT & Security infrastructure
├── exception/         # Global exception handling
└── cache/            # Redis cache configuration
```

## Configuration

### application.yml

Main configuration file with:
- Database connection
- Redis configuration
- JWT settings
- CORS configuration
- Storage settings
- Logging
- OpenAPI

### Profiles

- `dev` - Development (detailed logging, Swagger enabled)
- `prod` - Production (minimal logging, Swagger disabled)

Switch profiles:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## Development

### Code Style

- Use constructor injection only (no field injection)
- Follow SOLID principles
- Use Lombok to reduce boilerplate
- Apply validation annotations
- Use meaningful variable names

### Logging

Configured with SLF4J + Logback:

```java
@Slf4j
public class MyService {
    public void myMethod() {
        log.debug("Debug message");
        log.info("Info message");
        log.error("Error message", exception);
    }
}
```

## Testing

Run tests:

```bash
./mvnw test
```

## Building for Production

```bash
./mvnw clean package -DskipTests
```

The JAR file will be in `target/backend-0.0.1-SNAPSHOT.jar`

Run the JAR:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## Health Check

The application exposes actuator endpoints:

- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info
- **Metrics**: http://localhost:8080/actuator/metrics

## Environment Variables

Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |
| `SERVER_PORT` | Server port | `8080` |
| `DATABASE_URL` | PostgreSQL URL | `jdbc:postgresql://localhost:5432/ziboto` |
| `DATABASE_USERNAME` | Database username | `ziboto` |
| `DATABASE_PASSWORD` | Database password | `ziboto` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | JWT secret key | Required |
| `JWT_EXPIRATION` | Access token expiration (ms) | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:5173` |
| `STORAGE_TYPE` | Storage type (local/s3) | `local` |

## Security

- JWT-based authentication
- BCrypt password hashing
- CORS configuration
- Method-level security with `@PreAuthorize`
- Global exception handling
- Request validation

## Next Steps

1. Implement business logic in service classes
2. Complete JWT token generation and validation
3. Add integration tests
4. Implement file upload/download functionality
5. Add S3 storage implementation
6. Configure CI/CD pipeline
7. Add API rate limiting
8. Implement WebSocket for real-time features

## License

Apache 2.0
