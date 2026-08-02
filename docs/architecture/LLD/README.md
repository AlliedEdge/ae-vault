# Low-Level Design (LLD) Documentation

## Overview
This directory contains detailed Low-Level Design documentation for the Ziboto platform. Each document provides implementation-level details including class diagrams, database schemas, API specifications, algorithms, and code examples.

## Document Structure

### 1. [Authentication & Authorization Flow](01-authentication-flow.md)
**Purpose**: Complete authentication system design  
**Contents**:
- JWT-based authentication flow
- Registration and login processes
- Token refresh mechanism
- Session management with Redis
- Security filter implementation
- Password hashing with BCrypt
- Database schema for users and tokens

**Key Components**:
- `AuthenticationController`
- `JwtTokenProvider`
- `SecurityConfig`
- `JwtAuthenticationFilter`

---

### 2. [File Management System](02-file-management-system.md)
**Purpose**: Core file operations and storage management  
**Contents**:
- File upload/download flows
- Multipart file handling
- S3 storage integration
- File metadata management
- Streaming downloads
- Duplicate detection (SHA-256)
- Storage quota management

**Key Components**:
- `FileController`
- `FileService`
- `S3StorageService`
- `MetadataService`

**Database Tables**:
- `file_metadata`
- `folders`

---

### 3. [Caching Strategy](03-caching-strategy.md)
**Purpose**: Redis caching implementation  
**Contents**:
- Cache-aside pattern
- Session caching
- File metadata caching
- Folder structure caching
- Storage statistics caching
- Search results caching
- Rate limiting with Redis
- Cache invalidation strategies
- Performance optimization

**Cache Categories**:
- Session cache (7 days TTL)
- File metadata (1 hour TTL)
- Folder tree (30 minutes TTL)
- Storage stats (5 minutes TTL)
- Search results (15 minutes TTL)
- Rate limiting (1 minute sliding window)

---

### 4. [Database Schema](04-database-schema.md)
**Purpose**: Complete PostgreSQL database design  
**Contents**:
- Entity-Relationship Diagram
- Table definitions with constraints
- Indexes for performance
- Foreign key relationships
- Triggers for automation
- Stored procedures
- Database functions
- Views for common queries
- Maintenance procedures

**Tables**:
- `users` - User account information
- `folders` - Hierarchical folder structure
- `file_metadata` - File information and S3 references
- `audit_logs` - Comprehensive audit trail
- `refresh_tokens` - JWT refresh token management

---

### 5. [API Specifications](05-api-specifications.md)
**Purpose**: REST API endpoint documentation  
**Contents**:
- Complete API endpoint definitions
- Request/response formats
- Authentication requirements
- Rate limiting rules
- Validation rules
- Error response formats
- HTTP status codes
- Query parameters
- Pagination

**Endpoint Categories**:
- Authentication (`/auth/*`)
- File Management (`/files/*`)
- Folder Management (`/folders/*`)
- User Management (`/users/*`)

---

### 6. [S3 Multipart Upload](06-s3-multipart-upload.md)
**Purpose**: Large file upload implementation  
**Contents**:
- AWS S3 multipart upload flow
- Upload session management
- Part-by-part upload handling
- Resume capability
- Progress tracking
- Complete/abort operations
- Redis session storage
- Error handling and retry logic

**Key Features**:
- Handles files >100MB efficiently
- Parallel part uploads
- Resume interrupted uploads
- Real-time progress updates
- Network resilience

**Components**:
- `MultipartUploadController`
- `MultipartUploadService`
- `UploadSession` (Redis cache)

---

### 7. [Production Upload Flow](07-production-upload-flow.md)
**Purpose**: Production-ready, real-world upload architecture  
**Contents**:
- Complete end-to-end flow with presigned URLs
- Direct client-to-S3 uploads
- JWT authentication integration
- Rate limiting with Redis
- Storage quota management
- Parallel chunk uploads
- Progress tracking and monitoring
- Security best practices

**Key Features**:
- **No backend bandwidth usage** (direct client-to-S3)
- **Presigned URLs** for secure, time-limited S3 access
- **Production-grade security** (JWT, rate limiting, validation)
- **High performance** (parallel uploads, connection pooling)
- **Reliable** (retry, resume, idempotent operations)
- **Scalable** (stateless backend, Redis sessions)

**Architecture Highlights**:
- Client authenticates → Gets JWT
- Client requests upload session → Gets presigned URLs
- Client uploads chunks directly to S3 (no backend proxy)
- Client notifies backend → Backend completes S3 multipart
- Metadata saved to PostgreSQL

---

## Cross-Document References

### Authentication Flow
- Uses **Database Schema** → `users`, `refresh_tokens` tables
- Uses **Caching Strategy** → Session cache, token cache
- Exposes **API Specifications** → `/auth/*` endpoints

### File Management
- Uses **Database Schema** → `file_metadata`, `folders` tables
- Uses **Caching Strategy** → File metadata cache, folder tree cache
- Uses **Authentication Flow** → JWT validation
- Uses **S3 Multipart Upload** → For large files >100MB
- Exposes **API Specifications** → `/files/*`, `/folders/*` endpoints

### S3 Multipart Upload
- Extends **File Management** → Large file handling
- Uses **Caching Strategy** → Upload session cache in Redis
- Uses **Authentication Flow** → JWT validation
- Exposes **API Specifications** → `/files/multipart/*` endpoints

### Production Upload Flow
- **Complete implementation** of File Management + Multipart Upload
- Integrates **Authentication Flow** → JWT tokens, session management
- Uses **Caching Strategy** → Redis for rate limiting and sessions
- Uses **Database Schema** → File metadata storage
- Uses **S3 Multipart Upload** → Presigned URLs, direct client-to-S3
- **Best Practices**: Security, performance, reliability, scalability

### Caching Strategy
- Supports **Authentication Flow** → Session storage
- Supports **File Management** → Metadata and folder caching
- Supports **API Specifications** → Rate limiting

---

## Implementation Guidelines

### 1. Start with Database
Begin by implementing the database schema from document #4:
```bash
1. Create PostgreSQL database
2. Run table creation scripts
3. Create indexes
4. Set up triggers and functions
5. Test with sample data
```

### 2. Build Authentication
Implement authentication from document #1:
```bash
1. Set up Spring Security
2. Create User entity and repository
3. Implement JwtTokenProvider
4. Create AuthenticationService
5. Build AuthenticationController
6. Configure SecurityFilterChain
```

### 3. Add Caching Layer
Implement Redis caching from document #3:
```bash
1. Configure Redis connection
2. Create RedisTemplate beans
3. Implement cache configuration
4. Add @Cacheable annotations
5. Test cache hit/miss rates
```

### 4. Implement File Management
Build file operations from document #2 and #6:
```bash
1. Configure AWS S3 client
2. Create S3StorageService
3. Implement FileService for standard uploads
4. Implement MultipartUploadService for large files
5. Build FileController
6. Build MultipartUploadController
7. Add standard multipart file handling (Spring)
8. Add S3 multipart upload for large files
9. Implement streaming downloads
```

### 5. Expose REST APIs
Follow specifications from document #5:
```bash
1. Implement all controller endpoints
2. Add request/response DTOs
3. Implement validation
4. Add error handling
5. Configure rate limiting
6. Test with Postman/cURL
```

---

## Design Patterns Used

### Architectural Patterns
- **Layered Architecture**: Controller → Service → Repository
- **Cache-Aside**: Check cache before database
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Request/response data transfer objects

### Security Patterns
- **JWT Authentication**: Stateless authentication
- **BCrypt Hashing**: Password security
- **Filter Chain**: Request filtering and validation

### Data Patterns
- **Soft Delete**: Mark as deleted instead of physical deletion
- **Audit Trail**: Track all important actions
- **Optimistic Locking**: Prevent concurrent update conflicts

---

## Technology Stack Summary

| Layer | Technology | Purpose |
|-------|------------|---------|
| **API** | Spring Boot 3.x, Spring Web | REST API framework |
| **Security** | Spring Security, JWT | Authentication & authorization |
| **Database** | PostgreSQL 15+ | Relational data storage |
| **Cache** | Redis 7+ | High-speed caching |
| **Storage** | AWS S3 | Object storage |
| **Serialization** | Jackson | JSON processing |
| **Validation** | Jakarta Validation | Input validation |
| **Connection Pool** | HikariCP | Database connection pooling |

---

## Performance Considerations

### Database Optimization
- Proper indexing on frequently queried columns
- Connection pooling (HikariCP)
- Query optimization with EXPLAIN ANALYZE
- Partitioning for audit_logs table
- Regular VACUUM and ANALYZE

### Caching Optimization
- Appropriate TTL values per cache type
- Redis connection pooling
- Pipeline operations for bulk operations
- Compression for large cached values
- Cache warming for popular data

### API Optimization
- Pagination for list endpoints
- Streaming for file downloads
- Async processing for background tasks
- Rate limiting to prevent abuse
- Response compression (gzip)

### Storage Optimization
- S3 multipart upload for large files
- Presigned URLs for direct downloads
- SHA-256 for duplicate detection
- Lifecycle policies for old data

---

## Security Considerations

### Authentication Security
- BCrypt with cost factor 12
- JWT with short expiration (15 min)
- Refresh token rotation
- Device tracking
- Session invalidation on logout

### API Security
- HTTPS only in production
- CORS configuration
- Input validation and sanitization
- SQL injection prevention (parameterized queries)
- XSS protection headers
- Rate limiting per user and endpoint

### Data Security
- Encrypted database connections
- S3 bucket encryption at rest
- Secure credential storage (environment variables)
- Audit logging for compliance
- Regular security updates

---

## Testing Strategy

### Unit Tests
- Mock dependencies with Mockito
- Test individual methods
- Cover edge cases and error conditions
- Target: 80%+ code coverage

### Integration Tests
- Use Testcontainers for PostgreSQL and Redis
- Test database transactions
- Test cache operations
- Test S3 interactions (LocalStack)

### API Tests
- Test all endpoints with different scenarios
- Test authentication and authorization
- Test error handling
- Test rate limiting
- Use Postman/Rest-Assured

### Performance Tests
- Load testing with k6 or JMeter
- Concurrent user simulations
- Database query performance
- Cache hit ratio monitoring
- API response time tracking

---

## Deployment Architecture

```
┌──────────────────────────────────────────────────┐
│                   AWS Cloud                       │
│                                                   │
│  ┌─────────────────────────────────────────────┐ │
│  │           Amazon VPC                        │ │
│  │                                             │ │
│  │  ┌─────────────────────────────────────┐   │ │
│  │  │  Public Subnet                      │   │ │
│  │  │                                     │   │ │
│  │  │  ┌──────────────────────────────┐  │   │ │
│  │  │  │     EC2 Instance             │  │   │ │
│  │  │  │                              │  │   │ │
│  │  │  │  ┌────────┐  ┌────────────┐ │  │   │ │
│  │  │  │  │ Nginx  │  │ Docker     │ │  │   │ │
│  │  │  │  │        │  │ Containers │ │  │   │ │
│  │  │  │  │        │  │            │ │  │   │ │
│  │  │  │  │        │  │ - Spring   │ │  │   │ │
│  │  │  │  │        │  │ - React    │ │  │   │ │
│  │  │  │  │        │  │ - Postgres │ │  │   │ │
│  │  │  │  │        │  │ - Redis    │ │  │   │ │
│  │  │  │  └────────┘  └────────────┘ │  │   │ │
│  │  │  └──────────────────────────────┘  │   │ │
│  │  └─────────────────────────────────────┘   │ │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│  ┌─────────────────────────────────────────────┐ │
│  │            Amazon S3 Bucket                 │ │
│  │         (File Object Storage)               │ │
│  └─────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
```

---

## Next Steps

1. **Database Setup**: Create PostgreSQL database and run schema scripts
2. **Spring Boot Project**: Initialize Spring Boot project with dependencies
3. **AWS Configuration**: Set up S3 bucket and IAM roles
4. **Redis Setup**: Configure Redis instance
5. **Authentication Implementation**: Build JWT authentication system
6. **File Management**: Implement file upload/download
7. **Frontend Development**: Build React application
8. **Docker Configuration**: Create Dockerfiles and docker-compose.yml
9. **Testing**: Write unit, integration, and API tests
10. **Deployment**: Deploy to AWS EC2

---

## Diagram Tools

These LLD documents include various diagrams. To edit or create new diagrams:

- **Text-based diagrams**: Created with ASCII art (can be edited in any text editor)
- **HLD Diagram**: `docs/architecture/HLD/hld-v1.svg` (Draw.io format)
- **Future diagrams**: Consider using:
  - Draw.io (diagrams.net)
  - Lucidchart
  - PlantUML (for code-generated diagrams)
  - Mermaid (Markdown-based diagrams)

---

## Maintenance

### Document Updates
- Update LLD documents when architecture changes
- Keep API specifications in sync with code
- Update database schema documentation after migrations
- Version control all changes

### Code-Documentation Sync
- Generate API documentation from code annotations (Swagger/OpenAPI)
- Keep database schema in sync with migrations
- Update error codes in both code and documentation

---

## Contributing

When adding new features:
1. Update relevant LLD document
2. Add new sections if needed
3. Update cross-references
4. Keep examples consistent
5. Update this README index

---

**Version**: 1.0  
**Last Updated**: 2026-08-02  
**Author**: Ziboto Team  
**Status**: Draft - Ready for Implementation
