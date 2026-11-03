# Backend Starter Template

A production-ready, reusable Spring Boot backend starter template with clean architecture, JWT authentication, and standardized patterns.

## Technology Stack

| Technology | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Maven | 3.9+ |
| Spring MVC | 6.2+ |
| Spring Data JPA | 3.4+ |
| Hibernate | 6.6+ |
| PostgreSQL | 16+ |
| Lombok | 1.18+ |
| Jakarta Validation | 3.1+ |
| Spring Security | 6.4+ |
| JWT (jjwt) | 0.12.6 |
| Flyway | 10.20+ |
| JUnit 5 | 5.10+ |
| Mockito | 5.11+ |
| Testcontainers | 1.20+ |

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/app/
│   │       ├── Application.java
│   │       │
│   │       ├── config/
│   │       │   ├── SecurityConfig.java
│   │       │   ├── CorsConfig.java
│   │       │   ├── AppConfig.java
│   │       │   └── WebMvcConfig.java
│   │       │
│   │       ├── common/
│   │       │   ├── response/
│   │       │   │   ├── ApiResponse.java
│   │       │   │   ├── ApiError.java
│   │       │   │   └── PageResponse.java
│   │       │   ├── exception/
│   │       │   │   ├── BusinessException.java
│   │       │   │   ├── ResourceNotFoundException.java
│   │       │   │   ├── DuplicateResourceException.java
│   │       │   │   ├── UnauthorizedException.java
│   │       │   │   ├── ForbiddenException.java
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   ├── filter/
│   │       │   │   ├── RequestIdFilter.java
│   │       │   │   ├── RequestIdContext.java
│   │       │   │   └── RequestIdInterceptor.java
│   │       │   ├── validation/
│   │       │   │   └── ValidationUtils.java
│   │       │   └── util/
│   │       │       └── AppUtils.java
│   │       │
│   │       ├── auth/
│   │       │   ├── controller/
│   │       │   │   └── AuthController.java
│   │       │   ├── service/
│   │       │   │   ├── AuthService.java
│   │       │   │   └── CustomUserDetailsService.java
│   │       │   ├── security/
│   │       │   │   ├── JwtTokenProvider.java
│   │       │   │   ├── JwtAuthenticationFilter.java
│   │       │   │   ├── JwtAuthenticationEntryPoint.java
│   │       │   │   └── CustomAccessDeniedHandler.java
│   │       │   ├── mapper/
│   │       │   │   └── AuthMapper.java
│   │       │   └── dto/
│   │       │       ├── LoginRequest.java
│   │       │       ├── RegisterRequest.java
│   │       │       └── AuthResponse.java
│   │       │
│   │       ├── user/
│   │       │   ├── controller/
│   │       │   │   └── UserController.java
│   │       │   ├── service/
│   │       │   │   └── UserService.java
│   │       │   ├── mapper/
│   │       │   │   └── UserMapper.java
│   │       │   ├── repository/
│   │       │   │   └── UserRepository.java
│   │       │   ├── entity/
│   │       │   │   └── User.java
│   │       │   └── dto/
│   │       │       └── UserResponse.java
│   │       │
│   │       ├── project/
│   │       │   ├── controller/
│   │       │   │   └── ProjectController.java
│   │       │   ├── service/
│   │       │   │   └── ProjectService.java
│   │       │   ├── mapper/
│   │       │   │   └── ProjectMapper.java
│   │       │   ├── repository/
│   │       │   │   └── ProjectRepository.java
│   │       │   ├── entity/
│   │       │   │   └── Project.java
│   │       │   └── dto/
│   │       │       ├── CreateProjectRequest.java
│   │       │       ├── UpdateProjectRequest.java
│   │       │       └── ProjectResponse.java
│   │       │
│   │       └── health/
│   │           └── HealthController.java
│   │
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/
│           └── migration/
│               ├── V1__create_users_table.sql
│               └── V2__create_projects_table.sql
│
└── test/
    └── java/com/example/app/
        ├── ApplicationTests.java
        ├── BaseIntegrationTest.java
        ├── auth/
        │   ├── controller/
        │   │   └── AuthControllerTest.java
        │   └── service/
        │       └── AuthServiceTest.java
        ├── project/
        │   ├── controller/
        │   │   └── ProjectControllerTest.java
        │   ├── service/
        │   │   └── ProjectServiceTest.java
        │   └── mapper/
        │       └── ProjectMapperTest.java
        └── user/
            ├── service/
            │   └── UserServiceTest.java
            └── mapper/
                └── UserMapperTest.java
```

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 16+ (for local development)
- Docker (for Testcontainers integration tests)

## Installation

```bash
# Clone the repository
git clone <repository-url>
cd backend-starter

# Build the project
./mvnw clean compile
```

## Configuration

### Environment Variables

Create a `.env` file or set the following environment variables:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/backend_starter_dev
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# JWT (use a strong secret in production!)
JWT_SECRET=your-base64-encoded-secret-at-least-256-bits

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### Application Profiles

- **dev** (`application-dev.yml`): Local development with debug logging
- **prod** (`application-prod.yml`): Production configuration with environment variables

## Database Setup

### Manual Migration Workflow

This template uses Flyway for database migrations with **manual execution only**. Migrations are **never** automatically applied on application startup.

```bash
# Generate migration files (done during development)
# Files are created in src/main/resources/db/migration/

# Apply migrations manually using Flyway CLI or Maven
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/backend_starter_dev \
  -Dflyway.user=postgres -Dflyway.password=postgres

# Or use Flyway CLI directly
flyway migrate -url=jdbc:postgresql://localhost:5432/backend_starter_dev \
  -user=postgres -password=postgres
```

### Database Safety Rules

- **Never** use `ddl-auto: update`, `create`, or `create-drop` in any environment
- Hibernate is configured with `ddl-auto: validate` only
- Flyway is configured with `enabled: false` to prevent auto-migration
- All schema changes must go through versioned SQL migration files
- Review and apply migrations manually

## Running Locally

```bash
# Start PostgreSQL (using Docker)
docker run -d \
  --name postgres-dev \
  -e POSTGRES_DB=backend_starter_dev \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine

# Apply migrations
./mvnw flyway:migrate

# Run the application
./mvnw spring-boot:run

# Or run with dev profile explicitly
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will be available at `http://localhost:8080`

## Build Commands

```bash
# Compile
./mvnw compile

# Package (creates JAR)
./mvnw package

# Package skipping tests
./mvnw package -DskipTests

# Clean install
./mvnw clean install
```

## Test Commands

```bash
# Run all tests
./mvnw test

# Run unit tests only
./mvnw test -Dtest=*Test

# Run integration tests (requires Docker)
./mvnw test -Dtest=*IntegrationTest

# Run specific test class
./mvnw test -Dtest=ProjectServiceTest
```

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Users (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get current user profile |
| GET | `/api/users/{id}` | Get user by ID (ADMIN or self) |
| GET | `/api/users` | List all users (ADMIN) |

### Projects (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create project |
| GET | `/api/projects/{id}` | Get project by ID |
| GET | `/api/projects` | List projects (paginated) |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project (ADMIN) |

### Health
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Application health |
| GET | `/api/health/ready` | Readiness probe |
| GET | `/api/health/live` | Liveness probe |
| GET | `/actuator/health` | Spring Boot Actuator health |
| GET | `/actuator/info` | Application info |

## Request/Response Format

### Standard Success Response
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Example Project"
  },
  "error": null,
  "requestId": "uuid-request-id",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Standard Error Response
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Project not found",
    "validationErrors": [
      {
        "field": "name",
        "message": "Project name is required",
        "rejectedValue": ""
      }
    ]
  },
  "requestId": "uuid-request-id",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Pagination Response
```json
{
  "success": true,
  "data": {
    "content": [...],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false,
    "empty": false
  },
  "error": null,
  "requestId": "uuid-request-id",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Authentication

### JWT Token Usage

Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

### Request ID Tracking

Each request receives a unique `X-Request-ID` header. Include this header in your requests for tracing:

```
X-Request-ID: your-custom-request-id
```

If not provided, a UUID will be generated automatically.

## Adding a New Feature

Follow this checklist to add a new feature following the established patterns:

1. **Create the feature package** under `com.example.app.<feature>/`
   ```
   <feature>/
   ├── controller/
   ├── service/
   ├── mapper/
   ├── repository/
   ├── entity/
   └── dto/
   ```

2. **Create the JPA Entity** in `entity/`
   - Use `@Entity`, `@Table`
   - Include `id`, `createdAt`, `updatedAt` with `@CreationTimestamp`/`@UpdateTimestamp`
   - Use `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for JPA

3. **Create the Repository** in `repository/`
   - Extend `JpaRepository<Entity, Long>`
   - Add custom query methods as needed

4. **Create DTOs** in `dto/`
   - `Create<Feature>Request.java` with validation annotations
   - `Update<Feature>Request.java` with validation annotations
   - `<Feature>Response.java` for API responses
   - Use Java records for simple DTOs

5. **Create the Mapper** in `mapper/`
   - `toEntity(CreateRequest)` - converts create request to entity
   - `updateEntity(Entity, UpdateRequest)` - updates entity from request
   - `toResponse(Entity)` - converts entity to response

6. **Create the Service** in `service/`
   - `@Service` with `@RequiredArgsConstructor`
   - Use `@Transactional` for write operations
   - Use `@Transactional(readOnly = true)` for read operations
   - Throw appropriate exceptions (`ResourceNotFoundException`, `DuplicateResourceException`)

7. **Create the Controller** in `controller/`
   - `@RestController` with `@RequestMapping("/api/<feature>s")`
   - Use `@Valid` on request bodies
   - Return `ResponseEntity<ApiResponse<...>>`
   - Include `HttpServletRequest` for request ID

8. **Create Flyway Migration** in `src/main/resources/db/migration/`
   - `V<next>__create_<feature>_table.sql`
   - Use lowercase snake_case for tables and columns

9. **Write Tests**
   - Unit tests for mapper and service
   - Controller tests with `@WebMvcTest` and `@WithMockUser`
   - Integration tests extending `BaseIntegrationTest`

10. **Verify**
    - Run `./mvnw clean test`
    - Run `./mvnw clean package`
    - Verify application starts

## Architecture Guidelines

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture documentation.

## Authentication Details

See [AUTHENTICATION.md](AUTHENTICATION.md) for authentication implementation details.

## Database Conventions

See [DATABASE.md](DATABASE.md) for database conventions and migration workflow.

## Agent Instructions

See [AGENTS.md](AGENTS.md) for comprehensive instructions for AI coding agents working with this codebase.

## License

MIT License