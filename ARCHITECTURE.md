# Architecture Documentation

This document explains the architectural decisions, patterns, and conventions used in this Spring Boot backend starter template.

## Overall Request Lifecycle

```
HTTP Request
    │
    ▼
RequestIdFilter (generates/extracts X-Request-ID)
    │
    ▼
DispatcherServlet → Controller (extracts request, validates @Valid)
    │
    ▼
Service (business logic, @Transactional)
    │
    ├── Mapper (DTO ↔ Entity conversion)
    │
    ▼
Repository (JPA persistence)
    │
    ▼
Database (PostgreSQL)
    │
    ▼
Response flows back through layers
    │
    ▼
GlobalExceptionHandler (if exception occurs)
    │
    ▼
ApiResponse wrapper + RequestId in header
    │
    ▼
HTTP Response
```

## Layer Responsibilities

### Controller Layer (`controller/`)

**Responsibilities:**
- Define REST endpoints with `@RestController` and mapping annotations
- Extract request data from path variables, query parameters, headers, and body
- Apply request validation using `@Valid` and Jakarta Bean Validation
- Call appropriate service methods
- Return standardized `ApiResponse<T>` with proper HTTP status codes
- Include request ID from `HttpServletRequest` attribute

**Constraints:**
- No business logic
- No direct repository access
- No JPA entity manipulation
- No complex mapping logic
- Use constructor injection for dependencies

### Service Layer (`service/`)

**Responsibilities:**
- Implement business logic and application rules
- Coordinate repository operations
- Manage transaction boundaries with `@Transactional`
- Apply business validations beyond DTO constraints
- Throw domain-specific exceptions (`ResourceNotFoundException`, `DuplicateResourceException`, etc.)
- Coordinate with other services when needed

**Transaction Rules:**
- Write operations: `@Transactional`
- Read operations: `@Transactional(readOnly = true)`
- Keep transactions short
- No `@Transactional` on controllers
- Understand proxy-based behavior (self-invocation bypasses transactions)

### Mapper Layer (`mapper/`)

**Responsibilities:**
- Convert request DTOs to entities (`toEntity(CreateRequest)`)
- Update entities from request DTOs (`updateEntity(Entity, UpdateRequest)`)
- Convert entities to response DTOs (`toResponse(Entity)`)
- Handle data transformations and formatting

**Constraints:**
- No business logic
- No repository access
- Simple, readable implementations
- Prefer manual mapping over MapStruct

### Repository Layer (`repository/`)

**Responsibilities:**
- Extend `JpaRepository<Entity, Long>`
- Define derived query methods
- Custom queries with `@Query` (JPQL) or native SQL
- Provide entity access for services

**Constraints:**
- No business logic
- No cross-domain orchestration
- Focus purely on persistence

### Entity Layer (`entity/`)

**Responsibilities:**
- JPA entity definitions with `@Entity`, `@Table`
- Database column mappings with `@Column`
- Relationship mappings (`@ManyToOne`, `@OneToMany`, etc.)
- Lifecycle callbacks (`@CreationTimestamp`, `@UpdateTimestamp`)
- Spring Security `UserDetails` implementation for User entity

**Constraints:**
- Never returned directly from controllers
- Separate from API DTOs
- Protected no-arg constructor for JPA
- Use lazy fetching for associations

## Feature Package Organization

Each feature follows package-by-feature structure:

```
com.example.app.<feature>/
├── controller/     # REST endpoints
├── service/        # Business logic
├── mapper/         # DTO ↔ Entity conversion
├── repository/     # Data access
├── entity/         # JPA entities
└── dto/            # Request/Response DTOs
```

This keeps related code together and makes features independently developable.

## Entity and DTO Separation

**Critical Rule:** JPA entities are **never** exposed through API responses.

```
┌─────────────────────────────────────────────────────────────┐
│                    API BOUNDARY                             │
├─────────────────────────────────────────────────────────────┤
│  Controller ←→ Mapper ←→ Entity ←→ Repository ←→ Database  │
│        ↑                                                    │
│        │                                                    │
│   DTOs (Request/Response)                                   │
│   - CreateRequest                                           │
│   - UpdateRequest                                           │
│   - Response                                                │
└─────────────────────────────────────────────────────────────┘
```

**Benefits:**
- API contract stability independent of database schema
- Control over exposed fields (security, versioning)
- Validation at API boundary
- Performance (avoid over-fetching, lazy loading issues)

## Service Orchestration

### Single Feature Operations

Service coordinates its own repositories:

```java
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) { ... }
}
```

### Multi-Feature Operations

Use an orchestrator service:

```java
@Service
@RequiredArgsConstructor
public class ProjectOrchestratorService {
    private final ProjectService projectService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public ProjectResponse createWithNotification(CreateProjectRequest request, Long userId) {
        // All operations in same transaction
        User user = userService.findEntityById(userId);
        ProjectResponse project = projectService.create(request);
        notificationService.notifyUser(user, "Project created");
        return project;
    }
}
```

**Rules:**
- Orchestrator coordinates other services, not repositories directly
- All participating operations share the same transaction
- Avoid deep service-to-service dependency chains

## Transaction Handling

### Declarative Transaction Management

Spring's `@Transactional` on service methods:

```java
@Service
public class ProjectService {

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        // Write operation - transactional
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        // Read operation - read-only transaction
    }

    @Transactional
    public void deleteById(Long id) {
        // Write operation
    }
}
```

### Transaction Propagation

- Default: `REQUIRED` (joins existing or creates new)
- Read-only: optimization hint for database
- Rollback: by default on unchecked exceptions only
- Use `rollbackFor` for checked exceptions if needed

### Common Pitfalls

1. **Self-invocation bypasses proxy**: Calling `@Transactional` method from within same class doesn't work
2. **Checked exceptions don't rollback**: Only `RuntimeException` and `Error` trigger rollback by default
3. **Long transactions**: Keep transactions short to avoid connection pool exhaustion

## Database Conventions

### Naming

| Database Object | Convention | Example |
|----------------|------------|---------|
| Tables | lowercase snake_case | `users`, `projects` |
| Columns | lowercase snake_case | `first_name`, `created_at` |
| Indexes | `idx_<table>_<column>` | `idx_users_email` |
| Foreign Keys | `fk_<table>_<ref_table>` | `fk_projects_owner_id` |

### Standard Columns

Every entity table includes:
- `id BIGSERIAL PRIMARY KEY`
- `created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP`

### JPA Mapping

```java
@Entity
@Table(name = "projects")
@Getter @Setter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### Indexes and Constraints

- Define in SQL migrations, not JPA annotations
- Unique constraints for business keys (email, etc.)
- Foreign keys for relationships
- Indexes on frequently queried columns

## Error Handling

### Exception Hierarchy

```
RuntimeException
├── BusinessException (base for domain exceptions)
│   ├── ResourceNotFoundException (404)
│   ├── DuplicateResourceException (409)
│   ├── UnauthorizedException (401)
│   └── ForbiddenException (403)
└── Validation/Framework exceptions (handled by GlobalExceptionHandler)
```

### Global Exception Handler

`GlobalExceptionHandler` with `@RestControllerAdvice` handles:
- Domain exceptions → appropriate HTTP status
- Validation errors → 400 with field-level details
- Authentication/Authorization → 401/403
- Data integrity → 409
- Unexpected → 500 (logged, generic message)

### Response Format

All errors follow consistent structure:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message",
    "validationErrors": [...]
  },
  "requestId": "uuid",
  "timestamp": "ISO_INSTANT"
}
```

## Security Architecture

### Stateless JWT

```
Client                          Server
  │                                │
  ├─ POST /api/auth/login ───────►│
  │  (email, password)            │
  │                                │
  │◄──── 200 OK (JWT token) ──────┤
  │                                │
  ├─ GET /api/projects ──────────►│
  │  Authorization: Bearer <jwt>  │
  │                                │
  │  JwtAuthenticationFilter      │
  │  ├─ Extract JWT               │
  │  ├─ Validate signature        │
  │  ├─ Parse claims              │
  │  ├─ Load UserDetails          │
  │  └─ Set SecurityContext       │
  │                                │
  │◄──── 200 OK (projects) ───────┤
```

### Security Filter Chain

1. `RequestIdFilter` - Request ID tracking
2. `CorsFilter` - CORS handling
3. `SecurityFilterChain` - Spring Security
   - Stateless session
   - JWT authentication filter
   - Authorization rules
   - Exception handling

### Method-Level Security

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) { ... }

@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) { ... }
```

## Request Tracking

### Request ID Flow

1. `RequestIdFilter` extracts `X-Request-ID` header or generates UUID
2. Stores in request attribute and `RequestIdContext` (ThreadLocal)
3. `RequestIdInterceptor` makes it available to controllers
4. Included in all `ApiResponse` responses
5. Included in log output via MDC or explicit logging
6. Cleared after request completion

### Usage

```java
@RestController
public class ProjectController {
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(
            @PathVariable Long id,
            HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");
        // or
        String requestId = RequestIdContext.getRequestId();
        return ResponseEntity.ok(ApiResponse.success(data, requestId));
    }
}
```

## Configuration Management

### Profile-Based Configuration

```
application.yml          # Base config (all environments)
application-dev.yml      # Development overrides
application-prod.yml     # Production overrides
```

### Environment Variables

All secrets via environment variables:
- `${JWT_SECRET}` - JWT signing key
- `${DATABASE_URL}` - JDBC URL
- `${DATABASE_USERNAME}` - DB username
- `${DATABASE_PASSWORD}` - DB password
- `${CORS_ALLOWED_ORIGINS}` - CORS origins

### Externalized Configuration

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000
  security:
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

## Observability

### Health Endpoints

- `/actuator/health` - Spring Boot health (DB, disk, etc.)
- `/actuator/info` - Application info
- `/api/health` - Custom health with request ID
- `/api/health/ready` - Kubernetes readiness probe
- `/api/health/live` - Kubernetes liveness probe

### Logging

- SLF4J with Logback
- `@Slf4j` on classes needing logging
- Structured logging with request ID
- No sensitive data in logs
- Levels: DEBUG (dev), INFO/WARN/ERROR (prod)

## Performance Considerations

### Database

- Connection pooling with HikariCP
- Lazy loading for associations
- Fetch joins for known access patterns
- Pagination with `Pageable` / `Page`
- Max page size limit (100)

### Caching

- Not included by default
- Add Spring Cache + Redis when needed
- Cache at service layer, not repository

### Async Processing

- `@EnableAsync` with custom `TaskExecutor`
- Use for non-critical background operations
- Proper error handling in async methods

## Extensibility Points

### Adding Features

Follow the feature checklist in AGENTS.md to maintain consistency.

### Custom Validators

Create annotation + `ConstraintValidator` for complex validation:

```java
@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
public @interface UniqueEmail { ... }
```

### Event Publishing

Not included by default. Add Spring Events or Kafka when needed.

## Deployment Considerations

### Container

- Multi-stage Docker build
- Non-root user
- Health checks for orchestration
- JVM tuning for containers

### Database Migrations

- Flyway migrations applied manually in CI/CD
- Never auto-migrate on startup
- Backward-compatible migrations preferred

### Configuration

- All secrets via environment variables
- Config maps for non-secret config
- Separate configs per environment