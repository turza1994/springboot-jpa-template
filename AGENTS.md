# AGENTS.md - Instructions for AI Coding Agents

This document provides comprehensive instructions for AI coding agents working with this Spring Boot backend starter template. Follow these guidelines strictly to maintain consistency, quality, and architectural integrity.

## Architecture Overview

### Layer Responsibilities

```
Controller → Service → Mapper → Repository → Database
```

Each layer has distinct responsibilities:

| Layer | Responsibilities | Must NOT |
|-------|-----------------|----------|
| **Controller** | Handle HTTP requests/responses, define endpoints, extract request data, apply validation, call services, return standardized responses | Contain business logic, directly access repositories, manipulate JPA entities, complex mapping |
| **Service** | Implement business logic, coordinate repositories, manage transactions, apply business validations | Directly access other features' repositories, contain persistence logic |
| **Mapper** | Convert request DTOs → entities, entities → response DTOs, data transformations | Contain business logic, access repositories |
| **Repository** | Handle database persistence, define queries, provide entity access | Contain business logic, cross-domain orchestration |

### Package Organization

Use **package-by-feature** organization:

```
com.example.app.<feature>/
├── controller/
├── service/
├── mapper/
├── repository/
├── entity/
└── dto/
```

Common infrastructure in `common/`, configuration in `config/`.

## Java and Naming Conventions

### Naming Rules

| Element | Convention | Example |
|---------|------------|---------|
| Package names | lowercase | `com.example.app.project` |
| Class names | PascalCase | `ProjectService` |
| Interface names | PascalCase | `ProjectRepository` |
| Method names | camelCase | `createProject()` |
| Variables | camelCase | `projectName` |
| Constants | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| Enum names | PascalCase | `ProjectStatus` |
| Enum values | UPPER_SNAKE_CASE | `IN_PROGRESS` |
| DTO classes | PascalCase | `CreateProjectRequest` |
| Entity classes | PascalCase, singular | `Project` |
| Database tables | lowercase snake_case | `projects` |
| Database columns | lowercase snake_case | `created_at` |
| YAML properties | kebab-case | `spring.datasource.url` |
| Environment variables | UPPER_SNAKE_CASE | `DATABASE_URL` |
| Test classes | Class name + Test | `ProjectServiceTest` |

### General Rules

- One public top-level class per Java file
- Java filenames must match public class names
- Package and directory names must be lowercase
- Use singular feature package names
- Use descriptive names; avoid `Helper`, `Manager`, `Utils` unless justified
- Follow standard Java formatting and import conventions
- Avoid wildcard imports
- Use explicit, meaningful variable names
- Avoid unnecessary abbreviations

## DTO Conventions

### Structure

Every feature has a `dto` directory with all DTO files directly inside:

```
project/
└── dto/
    ├── CreateProjectRequest.java
    ├── UpdateProjectRequest.java
    └── ProjectResponse.java
```

### Naming Patterns

- `Create<Feature>Request` - for creation requests
- `Update<Feature>Request` - for update requests
- `<Feature>Response` - for standard responses
- `<Feature>SummaryResponse` - for list/summary views
- `<Feature>DetailsResponse` - for detailed views

### Implementation

- Use **Java records** for simple request/response DTOs
- Use Jakarta Bean Validation annotations on request DTOs (`@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Min`, `@Max`, `@Pattern`)
- **Never expose JPA entities through API responses**
- Do not create unnecessary DTOs when existing ones suffice

## JPA and Hibernate Conventions

### Entity Rules

- Keep JPA entities **separate from API DTOs** - never return entities from controllers
- Use appropriate constructors
- Use protected no-argument constructor: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- Use explicit `@Table` and `@Column` mappings where useful
- Use appropriate primary-key generation (`GenerationType.IDENTITY`)
- Define database constraints and indexes where justified
- Use lazy fetching for associations (`@ManyToOne(fetch = FetchType.LAZY)`)
- Avoid unnecessary bidirectional relationships
- Avoid unnecessary cascading
- Avoid loading entire object graphs when only a few fields needed

### Lombok Usage

**Recommended:**
- `@Getter`
- `@Setter` (when needed)
- `@RequiredArgsConstructor`
- `@Slf4j`
- `@Builder` (where appropriate)
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for JPA entities

**Avoid:**
- `@Data` on JPA entities
- `@EqualsAndHashCode` on entities (use business key or ID only)
- `@ToString` that traverses relationships
- Exposing setters for every field without considering invariants

### Entity Relationships

- Avoid unnecessary relationships
- Avoid making every relationship bidirectional
- Use lazy fetching for associations
- Avoid accidental N+1 queries - use fetch joins, entity graphs, projections, or custom queries
- Be careful accessing lazy-loaded relationships outside transactions

### Entity Updates

- Prefer updating managed entities inside a transaction
- Avoid unnecessary entity recreation for updates
- Understand `save()`, `merge()`, dirty checking, and persistence context behavior
- Do not use bulk JPQL updates without understanding their effect on persistence context

### Query Conventions

- Spring Data JPA derived queries for simple operations
- JPQL with `@Query` for custom queries
- Native SQL when PostgreSQL-specific functionality needed
- Specifications only when dynamic filtering genuinely requires them
- Custom repository implementations for complex persistence operations

## Repository Responsibilities

- Handle database persistence only
- Define database queries
- Provide access to entities
- Implement custom database operations when required
- **Do not** put business logic in repositories
- **Do not** put cross-domain orchestration in repositories

## Service Responsibilities

- Implement application and business logic
- Coordinate repository operations
- Coordinate operations involving multiple services
- Manage transaction boundaries
- Apply business validations
- Handle application-level decisions

### Transaction Management

```java
// Write operations
@Transactional
public ResponseDto create(CreateRequest request) { ... }

// Read operations
@Transactional(readOnly = true)
public ResponseDto getById(Long id) { ... }
```

- Keep transactions as short as reasonably possible
- Use transactions when multiple database operations must succeed/fail together
- **Do not** put `@Transactional` on controllers
- Understand Spring's proxy-based transaction behavior
- Self-invocation does not trigger transactional proxy interception

### Service Orchestration

For operations involving multiple features:
- Use an orchestrator service when appropriate
- The orchestrator coordinates other services
- Do not directly access another feature's repository from unrelated services
- Ensure participating database operations use the same transaction
- Avoid unnecessary service-to-service dependency chains

## Mapper Responsibilities

- Convert request DTOs into entities when appropriate
- Convert entities into response DTOs
- Perform data transformations
- Keep mapping logic separate from controllers and repositories
- Use simple, readable mapper classes
- Prefer manual mapping; do not introduce MapStruct without strong justification

## Authentication and Authorization

### Stateless JWT Authentication

Implementation uses:
- Spring Security with `SecurityFilterChain`
- `SessionCreationPolicy.STATELESS`
- JWT signing/verification (HS256)
- BCrypt password hashing
- Role-based authorization (`@PreAuthorize`, `@EnableMethodSecurity`)

### Flow

1. User registers/logs in
2. Backend validates credentials
3. Password verified with BCrypt
4. Backend generates signed JWT
5. Client sends JWT with requests
6. Spring Security validates JWT
6. User info available via SecurityContext
7. Protected endpoints enforce auth/authorization

### What NOT to Implement

- Access/refresh token hybrid architecture
- Refresh token rotation
- Database-backed authentication sessions
- JWT blacklisting
- CSRF cookie/header architecture

### Authorization

- Simple role-based foundation (`USER`, `ADMIN`)
- Use `@PreAuthorize` for method-level security
- Protected endpoints cannot be accessed by supplying user ID in request
- Authenticated identity must come from verified security context

## Validation

### Layers

1. **DTO Validation** - Request structure and basic constraints (`@Valid`, `@NotBlank`, `@Email`, `@Size`, etc.)
2. **Service Validation** - Application and business rules
3. **Database Constraints** - Data integrity (unique, foreign key, not null)

### Custom Validation

Create custom validation annotations only when standard annotations are insufficient.

## Exception Handling

### Centralized Handling

Use `@RestControllerAdvice` with `GlobalExceptionHandler`:

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(...) { ... }

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<Void>> handleValidationException(...) { ... }
```

### Exception Types

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | Entity not found |
| `DuplicateResourceException` | 409 | Unique constraint violation |
| `BusinessException` | 400/409 | Generic business rule violation |
| `UnauthorizedException` | 401 | Authentication required |
| `ForbiddenException` | 403 | Access denied |
| Validation exceptions | 400 | Request validation failed |

### Response Format

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Project not found",
    "validationErrors": [...]
  },
  "requestId": "uuid",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## API Response Structure

### Standardized Response

All endpoints must return `ApiResponse<T>`:

```java
return ResponseEntity.ok(ApiResponse.success(data, requestId));
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, requestId));
return ResponseEntity.badRequest().body(ApiResponse.error(error, requestId));
```

### Success Response

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "requestId": "uuid",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Error Response

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
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Pagination Response

Use `PageResponse<T>` wrapper for paginated results.

## Database Migration Restrictions

### Critical Rules

1. **Never** use Hibernate automatic schema updates
2. **Never** use `ddl-auto: update`, `create`, or `create-drop`
3. Configure Hibernate with `ddl-auto: validate`
4. **Do not** automatically execute Flyway migrations on startup
5. Configure Flyway with `enabled: false`
6. **Generate migration files only** - apply manually
7. **Never** execute destructive database operations
8. **Do not** reset or recreate existing databases

### Migration Workflow

1. Create SQL migration file: `src/main/resources/db/migration/V<version>__<description>.sql`
2. Use lowercase snake_case: `V3__add_project_status.sql`
3. Review migration file
4. Apply manually: `./mvnw flyway:migrate` or Flyway CLI
4. Verify in database

## Testing Expectations

### Test Organization

```
test/
└── java/com/example/app/
    ├── <feature>/
    │   ├── controller/
    │   │   └── <Feature>ControllerTest.java
    │   ├── service/
    │   │   └── <Feature>ServiceTest.java
    │   └── mapper/
    │       └── <Feature>MapperTest.java
    └── BaseIntegrationTest.java
```

### Test Types

| Type | Coverage | Framework |
|------|----------|-----------|
| **Unit Tests** | Service logic, mappers, utilities, validation | JUnit 5, Mockito |
| **Controller Tests** | Successful requests, invalid requests, status codes, response structure, auth | `@WebMvcTest`, `@WithMockUser` |
| **Repository Tests** | Entity persistence, custom queries, constraints, pagination | `@DataJpaTest`, Testcontainers |
| **Integration Tests** | Database interaction, auth flow, protected endpoints | `@SpringBootTest`, Testcontainers |

### Testing Principles

- Do not use mocks for every test - choose testing level based on what needs verification
- Keep tests independent and deterministic
- Use `@WithMockUser` for authenticated controller tests
- Use `BaseIntegrationTest` for tests requiring real database
- Testcontainers for PostgreSQL integration tests

### Test Naming

- Test class: `<ClassName>Test`
- Test method: `methodName_Scenario_ExpectedResult` or descriptive names

## Configuration and Environment

### Profiles

- `application.yml` - Base configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides

### Secrets

**Never hardcode:**
- Database passwords
- JWT secrets
- API keys
- External service credentials
- Production URLs with sensitive info

Use environment variables: `${JWT_SECRET}`, `${DATABASE_URL}`, etc.

## Logging and Request Tracking

### Request ID

- Generated/accepted via `X-Request-ID` header
- Available via `RequestIdContext.getRequestId()`
- Included in API responses
- Included in relevant logs

### Logging Rules

- Use `@Slf4j` from Lombok
- **Never log:** passwords, JWT values, sensitive personal data, database credentials, full auth headers
- Use appropriate log levels (DEBUG for dev, WARN/ERROR for production)

## Adding a New Feature: Checklist

- [ ] Create feature package with all subdirectories
- [ ] Create JPA Entity with proper annotations
- [ ] Create Repository extending JpaRepository
- [ ] Create DTOs (CreateRequest, UpdateRequest, Response)
- [ ] Create Mapper with toEntity, updateEntity, toResponse
- [ ] Create Service with @Transactional methods
- [ ] Create Controller with @Valid, proper responses
- [ ] Create Flyway migration file
- [ ] Write unit tests for mapper and service
- [ ] Write controller tests with @WebMvcTest
- [ ] Add integration test if needed
- [ ] Run `./mvnw clean test` and verify all pass
- [ ] Run `./mvnw clean package` and verify build

## Code Quality Standards

- Run `./mvnw clean verify` before committing
- No compiler warnings
- Lombok annotation processing works correctly
- All tests pass
- Follow the architectural patterns strictly
- Do not overengineer - prefer straightforward, readable implementations
- Use Spring Boot conventions and dependency management

## Dependencies

Only use dependencies that provide real value. Do NOT add by default:
- Spring Modulith
- Redis, Kafka, RabbitMQ
- Elasticsearch
- Docker, Kubernetes
- Unnecessary mapping frameworks
- Unnecessary abstraction libraries
- WebFlux (this is Spring MVC)

Use Spring Boot-managed dependency versions wherever possible.