# Plan: Fix Deprecated Spring Boot Properties and Create API Testing Guide

## Overview
This plan addresses two tasks:
1. **Fix deprecated properties** in `application.yml` - migrate `server.error.include-message` and `server.error.include-binding-errors` to `spring.web.error.*`
2. **Create comprehensive curl testing guide** for all API endpoints

---

## Task 1: Fix Deprecated Properties in application.yml

### Current State (lines 32-36)
```yaml
server:
  port: 8080
  error:
    include-message: always
    include-binding-errors: always
```

### Required Changes
Move the error configuration from `server.error` to `spring.web.error`:

```yaml
server:
  port: 8080

spring:
  web:
    error:
      include-message: always
      include-binding-errors: always
```

### Rationale
- Spring Boot 3.x deprecated `server.error.*` properties in favor of `spring.web.error.*`
- This is a breaking change preparation for Spring Boot 4.x
- The functionality remains identical

### Files to Modify
- `src/main/resources/application.yml` - Main configuration file

---

## Task 2: Create API Testing Guide with curl Commands

### API Endpoints Summary

| Module | Endpoint | Method | Auth Required | Description |
|--------|----------|--------|---------------|-------------|
| Health | `/api/health` | GET | No | Basic health check |
| Health | `/api/health/ready` | GET | No | Readiness probe |
| Health | `/api/health/live` | GET | No | Liveness probe |
| Auth | `/api/auth/register` | POST | No | User registration |
| Auth | `/api/auth/login` | POST | No | User login |
| Users | `/api/users/me` | GET | Yes (JWT) | Get current user |
| Users | `/api/users/{id}` | GET | Yes (JWT) | Get user by ID |
| Users | `/api/users` | GET | Yes (ADMIN) | List all users (paginated) |
| Projects | `/api/projects` | POST | Yes (JWT) | Create project |
| Projects | `/api/projects/{id}` | GET | Yes (JWT) | Get project by ID |
| Projects | `/api/projects` | GET | Yes (JWT) | List all projects (paginated) |
| Projects | `/api/projects/{id}` | PUT | Yes (JWT) | Update project |
| Projects | `/api/projects/{id}` | DELETE | Yes (ADMIN) | Delete project |

### Request/Response DTOs

#### RegisterRequest
```json
{
  "firstName": "string (2-50 chars)",
  "lastName": "string (2-50 chars)",
  "email": "string (valid email)",
  "password": "string (8-100 chars)"
}
```

#### LoginRequest
```json
{
  "email": "string (valid email)",
  "password": "string (8-100 chars)"
}
```

#### AuthResponse
```json
{
  "accessToken": "string (JWT)",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": { "UserResponse object" }
}
```

#### CreateProjectRequest / UpdateProjectRequest
```json
{
  "name": "string (1-100 chars, required)",
  "description": "string (max 5000 chars, optional)"
}
```

#### ProjectResponse
```json
{
  "id": "long",
  "name": "string",
  "description": "string",
  "createdAt": "ISO 8601 timestamp",
  "updatedAt": "ISO 8601 timestamp"
}
```

#### UserResponse
```json
{
  "id": "long",
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "role": "USER|ADMIN",
  "enabled": "boolean",
  "createdAt": "ISO 8601 timestamp",
  "updatedAt": "ISO 8601 timestamp"
}
```

### Curl Test Commands

#### 1. Health Checks (No Auth)
```bash
# Basic health check
curl -X GET http://localhost:8080/api/health

# Readiness probe
curl -X GET http://localhost:8080/api/health/ready

# Liveness probe
curl -X GET http://localhost:8080/api/health/live
```

#### 2. Authentication (No Auth Required)
```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "password": "password123"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "password123"
  }'
```

#### 3. User Endpoints (Requires JWT)
```bash
# Get current user profile (requires Authorization header)
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Get user by ID (requires Authorization header)
curl -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer <JWT_TOKEN>"

# List all users (requires ADMIN role)
curl -X GET "http://localhost:8080/api/users?page=0&size=20" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

#### 4. Project Endpoints (Requires JWT)
```bash
# Create project
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "name": "My Project",
    "description": "Project description"
  }'

# Get project by ID
curl -X GET http://localhost:8080/api/projects/1 \
  -H "Authorization: Bearer <JWT_TOKEN>"

# List all projects (with pagination and sorting)
curl -X GET "http://localhost:8080/api/projects?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Update project
curl -X PUT http://localhost:8080/api/projects/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "name": "Updated Project Name",
    "description": "Updated description"
  }'

# Delete project (requires ADMIN role)
curl -X DELETE http://localhost:8080/api/projects/1 \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

### Test Script File
Create a reusable test script at `scripts/test-apis.sh`:

```bash
#!/bin/bash
# API Testing Script for Spring Boot Backend Template
# Usage: ./scripts/test-apis.sh

BASE_URL="http://localhost:8080"
JWT_TOKEN=""
ADMIN_JWT_TOKEN=""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=== Health Checks ==="
curl -s -X GET "$BASE_URL/api/health" | jq .
curl -s -X GET "$BASE_URL/api/health/ready" | jq .
curl -s -X GET "$BASE_URL/api/health/live" | jq .

echo -e "\n=== Authentication ==="
# Register user
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","password":"password123"}')
echo "Register: $REGISTER_RESPONSE"

# Login
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}')
echo "Login: $LOGIN_RESPONSE"

# Extract JWT token
JWT_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')
echo -e "${GREEN}JWT Token: $JWT_TOKEN${NC}"

echo -e "\n=== User Endpoints ==="
curl -s -X GET "$BASE_URL/api/users/me" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq .

echo -e "\n=== Project Endpoints ==="
# Create project
CREATE_PROJECT=$(curl -s -X POST "$BASE_URL/api/projects" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"name":"Test Project","description":"A test project"}')
echo "Create Project: $CREATE_PROJECT"

# Extract project ID
PROJECT_ID=$(echo $CREATE_PROJECT | jq -r '.data.id')

# Get project
curl -s -X GET "$BASE_URL/api/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq .

# List projects
curl -s -X GET "$BASE_URL/api/projects?page=0&size=10" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq .

# Update project
curl -s -X PUT "$BASE_URL/api/projects/$PROJECT_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"name":"Updated Project","description":"Updated description"}' | jq .

# Delete project (requires admin - will fail with regular user)
curl -s -X DELETE "$BASE_URL/api/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq .
```

### Validation Steps
1. Apply the configuration fix to `application.yml`
2. Start the application: `./mvnw spring-boot:run`
3. Run the health checks to verify the application starts correctly
4. Execute the curl commands in sequence to test all endpoints
5. Verify error responses include message and binding errors as expected

---

## Implementation Order
1. **First**: Fix the deprecated properties in `application.yml`
2. **Second**: Create the test script file `scripts/test-apis.sh`
3. **Third**: Document the curl commands in a README or testing guide
4. **Fourth**: Run the application and execute tests to validate

---

## Risks & Considerations
- The `server.error` properties are deprecated but may still work in Spring Boot 3.x - they will be removed in 4.x
- JWT tokens expire after 24 hours (86400000 ms) - tests should account for this
- Admin endpoints require a user with ADMIN role - may need to create one manually in DB
- The application uses PostgreSQL - ensure database is running before tests

---

## Success Criteria
- [ ] `application.yml` uses `spring.web.error.*` properties instead of deprecated `server.error.*`
- [ ] Application starts without deprecation warnings for these properties
- [ ] All health endpoints return 200 OK
- [ ] Auth endpoints work for register/login flow
- [ ] Protected endpoints return 401 without JWT, 200 with valid JWT
- [ ] Error responses include message and binding errors when validation fails