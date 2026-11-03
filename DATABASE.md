# Database Documentation

This document describes the PostgreSQL database conventions, JPA/Hibernate configuration, entity patterns, and migration workflow for this Spring Boot backend starter template.

## PostgreSQL Configuration

### Connection Properties

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
```

### Hibernate/JPA Configuration

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
```

**Critical:** `ddl-auto: validate` only. Never use `update`, `create`, or `create-drop`.

## Entity Conventions

### Base Entity Pattern

All entities follow this structure:

```java
@Entity
@Table(name = "entity_name")
@Getter @Setter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EntityName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Business fields with explicit @Column

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### Annotations Used

| Annotation | Purpose |
|------------|---------|
| `@Entity` | Marks class as JPA entity |
| `@Table(name = "...")` | Explicit table name (lowercase snake_case) |
| `@Id` | Primary key |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Auto-increment (PostgreSQL BIGSERIAL) |
| `@Column(name = "...", ...)` | Explicit column mapping |
| `@CreationTimestamp` | Auto-set on persist |
| `@UpdateTimestamp` | Auto-set on persist and update |
| `@Enumerated(EnumType.STRING)` | Store enum as string |
| `@ManyToOne(fetch = FetchType.LAZY)` | Lazy-loaded association |
| `@OneToMany(mappedBy = "...")` | Inverse side of relationship |

### Lombok on Entities

**Use:**
- `@Getter` `@Setter` (when needed)
- `@Builder` (for test construction)
- `@RequiredArgsConstructor` (for required fields)
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` (JPA requirement)
- `@AllArgsConstructor` (for full construction)

**Avoid:**
- `@Data` (includes `@EqualsAndHashCode`, `@ToString` which cause issues)
- `@EqualsAndHashCode` (use ID only if needed)
- `@ToString` (can trigger lazy loading)

## Database Naming Conventions

| Object | Convention | Example |
|--------|------------|---------|
| Tables | lowercase snake_case, plural | `users`, `projects` |
| Columns | lowercase snake_case | `first_name`, `created_at` |
| Primary Keys | `id` (BIGSERIAL) | `id BIGSERIAL PRIMARY KEY` |
| Foreign Keys | `<entity>_id` | `user_id`, `project_id` |
| Indexes | `idx_<table>_<column>` | `idx_users_email` |
| Unique Constraints | `uk_<table>_<column>` | `uk_users_email` |
| Foreign Key Constraints | `fk_<table>_<ref_table>` | `fk_projects_owner_id` |

### Standard Columns

Every table includes:
```sql
id BIGSERIAL PRIMARY KEY,
created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
```

## Migration Management

### Flyway Configuration

```yaml
spring:
  flyway:
    enabled: false                    # Never auto-run on startup
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration
```

### Migration File Naming

```
V<version>__<description>.sql
```

Examples:
- `V1__create_users_table.sql`
- `V2__create_projects_table.sql`
- `V3__add_project_status_column.sql`
- `V4__create_index_on_users_email.sql`

**Rules:**
- Version numbers: sequential integers (1, 2, 3...)
- Description: lowercase snake_case
- Double underscore `__` separates version from description
- Single underscore `_` within description

### Migration Workflow

#### During Development

1. Make entity changes
2. Create SQL migration file in `src/main/resources/db/migration/`
3. Test migration locally:
   ```bash
   ./mvnw flyway:migrate \
     -Dflyway.url=jdbc:postgresql://localhost:5432/backend_starter_dev \
     -Dflyway.user=postgres -Dflyway.password=postgres
   ```
4. Verify schema matches entities
5. Commit migration file

#### In CI/CD

```bash
# Apply migrations before deployment
./mvnw flyway:migrate \
  -Dflyway.url=${DATABASE_URL} \
  -Dflyway.user=${DATABASE_USERNAME} \
  -Dflyway.password=${DATABASE_PASSWORD}
```

#### Rollback Strategy

- Flyway doesn't support automatic rollback
- Create compensating migration (e.g., `V5__revert_add_status.sql`)
- Or restore from database backup

### Writing Migrations

#### Create Table

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);
```

#### Add Column

```sql
-- V3__add_project_status.sql
ALTER TABLE projects ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_projects_status ON projects(status);
```

#### Add Index

```sql
-- V4__create_index_on_users_last_login.sql
CREATE INDEX idx_users_last_login ON users(last_login_at);
```

#### Data Migration

```sql
-- V5__migrate_user_roles.sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

### Migration Best Practices

1. **One migration per logical change**
2. **Never modify applied migrations** - create new ones
3. **Test migrations** on copy of production data
4. **Use transactions** - Flyway wraps each migration in transaction
5. **Idempotent when possible** - use `IF NOT EXISTS`, `IF EXISTS`
6. **Backward compatible** - avoid breaking changes in same deployment

## Schema Validation

### Hibernate Validation

On startup, Hibernate validates:
- Entity-to-table mapping
- Column types match
- Constraints exist
- Indexes match (optional)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Fails fast on mismatch
```

### Common Validation Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Table 'xxx' not found` | Missing migration | Run Flyway migrate |
| `Column 'xxx' not found` | Entity has field not in DB | Add column via migration |
| `Column type mismatch` | JPA type ≠ DB type | Fix `@Column(columnDefinition)` or migration |
| `Missing index` | `@Index` in entity not in DB | Add index via migration |

## JPA/Hibernate Patterns

### Query Methods

**Derived Queries (Repository):**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleOrderByCreatedAtDesc(Role role);
    Page<User> findAllByEnabledTrue(Pageable pageable);
}
```

**Custom JPQL Queries:**
```java
@Query("SELECT u FROM User u WHERE u.email = :email AND u.enabled = true")
Optional<User> findActiveByEmail(@Param("email") String email);

@Query("SELECT u FROM User u WHERE u.createdAt > :since")
List<User> findUsersCreatedAfter(@Param("since") Instant since);
```

**Native SQL Queries:**
```java
@Query(value = "SELECT * FROM users WHERE email = ?1", nativeQuery = true)
Optional<User> findByEmailNative(String email);
```

### Fetching Strategies

**Default: LAZY for associations**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_id")
private User owner;
```

**Fetch Joins for known access patterns:**
```java
@Query("SELECT p FROM Project p JOIN FETCH p.owner WHERE p.id = :id")
Optional<Project> findWithOwner(@Param("id") Long id);
```

**Entity Graphs:**
```java
@EntityGraph(attributePaths = {"owner"})
Optional<Project> findById(Long id);
```

### Projections

**Interface-based:**
```java
public interface ProjectSummary {
    Long getId();
    String getName();
    Instant getCreatedAt();
}

List<ProjectSummary> findAllProjectSummaries();
```

**DTO-based:**
```java
@Query("SELECT new com.example.app.project.dto.ProjectSummaryResponse(p.id, p.name, p.createdAt) FROM Project p")
List<ProjectSummaryResponse> findAllSummaries();
```

### Pagination

```java
Page<Project> findAll(Pageable pageable);

// Controller:
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> getAll(
        @PageableDefault(size = 20, maxSize = 100) Pageable pageable) { ... }
```

### Locking

**Pessimistic Locking:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Project p WHERE p.id = :id")
Optional<Project> findForUpdate(@Param("id") Long id);
```

**Optimistic Locking:**
```java
@Version
private Long version;  // Add to entity when needed
```

Use optimistic locking only when specific requirement justifies it.

## Database Safety Rules

### Never Do

1. ❌ `ddl-auto: update` / `create` / `create-drop`
2. ❌ Auto-run Flyway on startup (`enabled: true`)
3. ❌ Execute DDL in application code
4. ❌ Drop tables/columns in migrations without rollback plan
4. ❌ Modify production data in migrations
5. ❌ Hardcode database credentials

### Always Do

1. ✅ `ddl-auto: validate`
2. ✅ Flyway `enabled: false`
3. ✅ Versioned SQL migrations only
4. ✅ Review migrations before applying
5. ✅ Apply migrations manually in all environments
6. ✅ Use environment variables for credentials
7. ✅ Test migrations on staging copy of production data
8. ✅ Backup database before migrations

## Connection Pooling

### HikariCP Configuration

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1800000
      validation-timeout: 5000
      leak-detection-threshold: 60000
```

### Pool Sizing Guidelines

| Environment | Max Pool | Min Idle |
|-------------|----------|----------|
| Development | 5-10 | 2-5 |
| Staging | 10-20 | 5-10 |
| Production | 20-50 | 10-20 |

## Monitoring and Maintenance

### Health Checks

Spring Boot Actuator checks:
- Database connectivity
- Connection pool status
- Disk space

```bash
curl http://localhost:8080/actuator/health
```

### Slow Query Detection

Enable in development:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
logging:
  level:
    org.hibernate.stat: DEBUG
```

### Connection Pool Metrics

Monitor via Actuator:
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.idle
```

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `ddl-auto: validate` fails | Schema mismatch | Run pending migrations |
| `Connection refused` | DB not running | Start PostgreSQL |
| `Authentication failed` | Wrong credentials | Check env vars |
| `Pool exhausted` | Leak or undersized | Fix leaks, increase pool |
| `Deadlock` | Lock ordering | Consistent lock order, indexes |
| `Slow queries` | Missing indexes | Analyze with `EXPLAIN ANALYZE` |

### Debugging Queries

Enable SQL logging in development:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## Backup and Recovery

### Logical Backup (pg_dump)

```bash
pg_dump -h localhost -U postgres -d backend_starter_dev > backup.sql
```

### Restore

```bash
psql -h localhost -U postgres -d backend_starter_dev < backup.sql
```

### Point-in-Time Recovery

Configure PostgreSQL WAL archiving for production.

## Extending the Schema

When adding new features:

1. Create entity in `feature/entity/`
2. Create repository in `feature/repository/`
3. Create migration `V<next>__create_<feature>_table.sql`
4. Run migration locally to verify
5. Update entity if migration reveals issues
6. Commit both entity and migration

### Adding Columns to Existing Tables

```sql
-- Safe: Add nullable column
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20);

-- Safe: Add column with default
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Unsafe: Add NOT NULL without default (fails on existing rows)
-- ALTER TABLE users ADD COLUMN middle_name VARCHAR(50) NOT NULL;
```

### Removing Columns

```sql
-- Safe: Mark deprecated, remove in later migration
-- ALTER TABLE users DROP COLUMN deprecated_column;
```

**Never drop columns directly in production without:**
1. Deprecating in code first
2. Waiting for deployment
3. Then creating drop migration

## Performance Optimization

### Indexing Strategy

1. **Primary keys** - automatic
2. **Foreign keys** - explicit indexes
3. **Query patterns** - indexes on WHERE/ORDER BY columns
4. **Unique constraints** - automatic indexes

### Query Optimization

1. Use `EXPLAIN ANALYZE` to understand query plans
2. Avoid `SELECT *` - use projections
3. Paginate large result sets
4. Use fetch joins instead of N+1 queries
5. Consider materialized views for complex aggregations

### Partitioning (Advanced)

For very large tables:
```sql
-- Range partitioning by created_at
CREATE TABLE projects_partitioned (
    LIKE projects INCLUDING ALL
) PARTITION BY RANGE (created_at);
```

Not needed for typical applications.

## Testing with Database

### Unit Tests (H2 In-Memory)

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {
    @Autowired UserRepository userRepository;
    // Test with H2
}
```

### Integration Tests (Testcontainers)

```java
@SpringBootTest
@Testcontainers
class UserIntegrationTest extends BaseIntegrationTest {
    @Autowired UserRepository userRepository;
    // Test with real PostgreSQL
}
```

### Repository Test Patterns

```java
@Test
void findByEmail_ShouldReturnUser() {
    // Given
    User user = User.builder().email("test@example.com").build();
    userRepository.save(user);

    // When
    Optional<User> found = userRepository.findByEmail("test@example.com");

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("test@example.com");
}
```

## Environment-Specific Configuration

### Development

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
  datasource:
    url: jdbc:postgresql://localhost:5432/backend_starter_dev
    username: postgres
    password: postgres
```

### Production

```yaml
# application-prod.yml
spring:
  jpa:
    show-sql: false
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
```

### CI/CD

Use Testcontainers for isolated test databases:
```java
@Container
@ServiceConnection
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
```