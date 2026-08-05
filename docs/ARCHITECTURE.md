# Ziboto Backend Architecture

## Overview

Ziboto backend is a production-grade Spring Boot 3 application following **Package-by-Feature** architecture with clear separation of concerns.

## Architectural Principles

### 1. Package-by-Feature
Each business feature is self-contained in its own package with all necessary layers.

### 2. Layered Architecture
Within each feature:
- **Controller Layer**: REST API endpoints
- **Service Layer**: Business logic
- **Repository Layer**: Data access
- **Entity Layer**: JPA entities
- **DTO Layer**: Data transfer objects
- **Mapper Layer**: Entity-DTO conversions

### 3. SOLID Principles
- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Open for extension, closed for modification
- **Liskov Substitution**: Subtypes must be substitutable
- **Interface Segregation**: Many specific interfaces over one general
- **Dependency Inversion**: Depend on abstractions, not concretions

### 4. Dependency Injection
- Constructor injection only (no field injection)
- Promotes testability and immutability
- Clear dependencies in constructor signature

## Project Structure

```
com.ziboto.backend
│
├── BackendApplication.java          # Main application entry point
│
├── auth/                             # Authentication Feature
│   ├── controller/
│   │   └── AuthController.java      # Login, Register, Refresh, Logout
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── AuthServiceImpl.java
│   │   └── CustomUserDetailsService.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       ├── AuthResponse.java
│       └── RefreshTokenRequest.java
│
├── user/                             # User Management Feature
│   ├── controller/
│   │   └── UserController.java      # CRUD operations
│   ├── service/
│   │   ├── UserService.java
│   │   └── UserServiceImpl.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── UserRole.java
│   │   └── UserStatus.java
│   ├── dto/
│   │   ├── UserResponse.java
│   │   └── UpdateUserRequest.java
│   └── mapper/
│       └── UserMapper.java
│
├── storage/                          # Bucket Management Feature
│   ├── service/
│   │   ├── BucketService.java
│   │   └── BucketServiceImpl.java
│   ├── repository/
│   │   └── BucketRepository.java
│   └── entity/
│       ├── Bucket.java
│       ├── BucketVisibility.java
│       └── BucketStatus.java
│
├── file/                             # File Management Feature
│   ├── service/
│   │   ├── FileStorageService.java
│   │   └── LocalFileStorageService.java
│   ├── repository/
│   │   └── FileMetadataRepository.java
│   └── entity/
│       ├── FileMetadata.java
│       └── FileStatus.java
│
├── audit/                            # Audit Logging Feature
│   ├── service/
│   │   ├── AuditService.java
│   │   └── AuditServiceImpl.java
│   ├── repository/
│   │   └── AuditLogRepository.java
│   └── entity/
│       ├── AuditLog.java
│       └── AuditAction.java
│
├── common/                           # Shared Infrastructure
│   ├── dto/
│   │   ├── ApiResponse.java         # Standard API response wrapper
│   │   └── PageResponse.java        # Paginated response wrapper
│   ├── entity/
│   │   └── BaseEntity.java          # Base entity with audit fields
│   └── constant/
│       └── ErrorCode.java           # Application error codes
│
├── config/                           # Configuration Layer
│   ├── OpenApiConfig.java           # Swagger/OpenAPI configuration
│   ├── JpaConfig.java               # JPA auditing configuration
│   ├── CorsConfig.java              # CORS configuration
│   └── properties/
│       └── AppProperties.java       # Application properties binding
│
├── security/                         # Security Infrastructure
│   ├── SecurityConfig.java          # Spring Security configuration
│   ├── JwtTokenProvider.java        # JWT token generation/validation
│   ├── JwtAuthenticationFilter.java # JWT request filter
│   └── JwtAuthenticationEntryPoint.java
│
├── exception/                        # Exception Handling
│   ├── GlobalExceptionHandler.java  # Global exception handler
│   ├── BaseException.java           # Base application exception
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   ├── ValidationException.java
│   └── ConflictException.java
│
└── cache/                            # Cache Layer
    ├── RedisConfig.java             # Redis configuration
    └── CacheService.java            # Cache abstraction service
```

## Data Flow

### 1. Request Flow
```
Client Request
    ↓
Controller (Validation)
    ↓
Service (Business Logic)
    ↓
Repository (Data Access)
    ↓
Database
```

### 2. Response Flow
```
Database
    ↓
Entity
    ↓
Mapper (Entity → DTO)
    ↓
ApiResponse Wrapper
    ↓
Client Response
```

### 3. Authentication Flow
```
Login Request
    ↓
AuthController
    ↓
AuthenticationManager
    ↓
CustomUserDetailsService
    ↓
UserRepository
    ↓
JWT Token Generation
    ↓
AuthResponse (with tokens)
```

### 4. JWT Filter Flow
```
Request with JWT
    ↓
JwtAuthenticationFilter
    ↓
Extract & Validate Token
    ↓
Load UserDetails
    ↓
Set Authentication in SecurityContext
    ↓
Proceed to Controller
```

## Database Schema

### Entity Relationships

```
User (1) ←→ (N) Bucket
User (1) ←→ (N) FileMetadata
User (1) ←→ (N) AuditLog
Bucket (1) ←→ (N) FileMetadata
```

### Audit Strategy
- All entities extend `BaseEntity` with:
  - `createdAt`, `updatedAt`
  - `createdBy`, `lastModifiedBy`
  - `version` (optimistic locking)
- Audit information is automatically populated via JPA auditing

## Security Architecture

### Authentication
- JWT-based stateless authentication
- Access tokens (short-lived, 24 hours)
- Refresh tokens (long-lived, 7 days)
- BCrypt password hashing

### Authorization
- Role-based access control (RBAC)
- Method-level security with `@PreAuthorize`
- Roles: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`

### Security Filters
1. **JwtAuthenticationFilter**: Validates JWT on each request
2. **SecurityFilterChain**: Configures security rules
3. **JwtAuthenticationEntryPoint**: Handles unauthorized access

## Caching Strategy

### Redis Cache
- User sessions
- Authentication tokens (blacklist for logout)
- Frequently accessed data
- Configurable TTL per cache region

### Cache Patterns
- Cache-Aside (Lazy Loading)
- Write-Through for critical data
- Eviction on entity updates/deletes

## Exception Handling

### Global Exception Handler
Catches and transforms exceptions into standardized API responses:

```json
{
  "success": false,
  "message": "Error message",
  "errors": { ... },
  "timestamp": "2024-01-01T12:00:00"
}
```

### Exception Hierarchy
```
BaseException (abstract)
    ├── ResourceNotFoundException
    ├── UnauthorizedException
    ├── ValidationException
    └── ConflictException
```

## API Design

### RESTful Conventions
- `GET /api/v1/users` - List users
- `GET /api/v1/users/{id}` - Get user by ID
- `POST /api/v1/users` - Create user
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user

### Standard Response Format
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2024-01-01T12:00:00"
}
```

### Pagination
```json
{
  "content": [...],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false,
  "empty": false
}
```

## Validation

### Input Validation
- Jakarta Bean Validation annotations
- `@NotBlank`, `@Email`, `@Size`, `@Pattern`
- Custom validators when needed
- Validation errors mapped to field-level errors

### Business Validation
- Service layer validates business rules
- Throws appropriate exceptions
- Logged for audit purposes

## Logging Strategy

### Logging Levels
- **DEBUG**: Development details
- **INFO**: Important business events
- **WARN**: Potential issues
- **ERROR**: Errors with stack traces

### Structured Logging
```java
log.info("User created: userId={}, username={}", user.getId(), user.getUsername());
```

## Database Migration

### Flyway
- Version-controlled migrations in `db/migration/`
- Naming: `V{version}__{description}.sql`
- Runs automatically on startup
- Rollback support via versioned scripts

## Deployment Considerations

### Health Checks
- `/actuator/health` - Application health
- `/actuator/info` - Application info
- `/actuator/metrics` - Performance metrics

### Environment Profiles
- **dev**: Development with detailed logging
- **prod**: Production with minimal logging
- Profile-specific configurations

### Scalability
- Stateless design for horizontal scaling
- Redis for shared state
- Database connection pooling
- Async processing where appropriate

## Best Practices

1. **Constructor Injection**: Required dependencies via constructor
2. **Interface Segregation**: Service interfaces for flexibility
3. **DTO Pattern**: Never expose entities in API
4. **Exception Translation**: Domain exceptions → HTTP responses
5. **Transaction Management**: `@Transactional` on service methods
6. **Validation**: Controller validates input, service validates business rules
7. **Logging**: Consistent structured logging
8. **Documentation**: OpenAPI/Swagger for API docs
9. **Testing**: Unit tests for services, integration tests for APIs
10. **Security**: Never log sensitive data (passwords, tokens)

## Future Enhancements

1. Multi-tenancy support
2. WebSocket for real-time updates
3. Rate limiting
4. API versioning strategy
5. Event-driven architecture with Kafka
6. Distributed tracing with Zipkin
7. S3-compatible storage implementation
8. Advanced search with Elasticsearch
