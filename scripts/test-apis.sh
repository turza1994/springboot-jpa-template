#!/bin/bash
# API Testing Script for Spring Boot Backend Template
# Usage: ./scripts/test-apis.sh
# Requires: jq (for JSON parsing), curl

set -e

BASE_URL="http://localhost:8080"
JWT_TOKEN=""
ADMIN_JWT_TOKEN=""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=== Spring Boot Backend Template - API Testing ==="
echo "Base URL: $BASE_URL"
echo ""

# Function to check if jq is installed
check_jq() {
    if ! command -v jq &> /dev/null; then
        echo -e "${YELLOW}Warning: jq not installed. JSON output will not be formatted.${NC}"
        echo "Install with: sudo apt-get install jq (Linux) or brew install jq (macOS)"
        echo ""
        JQ_AVAILABLE=false
    else
        JQ_AVAILABLE=true
    fi
}

# Function to format JSON if jq available
format_json() {
    if [ "$JQ_AVAILABLE" = true ]; then
        jq .
    else
        cat
    fi
}

check_jq

echo "=== Health Checks (No Auth Required) ==="

echo -e "${YELLOW}1. Basic Health Check${NC}"
curl -s -X GET "$BASE_URL/api/health" | format_json

echo -e "\n${YELLOW}2. Readiness Probe${NC}"
curl -s -X GET "$BASE_URL/api/health/ready" | format_json

echo -e "\n${YELLOW}3. Liveness Probe${NC}"
curl -s -X GET "$BASE_URL/api/health/live" | format_json

echo -e "\n=== Authentication (No Auth Required) ==="

echo -e "${YELLOW}4. Register New User${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "password": "password123"
  }')
echo "$REGISTER_RESPONSE" | format_json

echo -e "\n${YELLOW}5. Login${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')
echo "$LOGIN_RESPONSE" | format_json

# Extract JWT token
if [ "$JQ_AVAILABLE" = true ]; then
    JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken // empty')
else
    # Fallback extraction without jq
    JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
fi

if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
    echo -e "${GREEN}JWT Token extracted successfully${NC}"
else
    echo -e "${RED}Failed to extract JWT token${NC}"
    JWT_TOKEN=""
fi

echo -e "\n=== User Endpoints (Requires JWT) ==="

if [ -n "$JWT_TOKEN" ]; then
    echo -e "${YELLOW}6. Get Current User Profile${NC}"
    curl -s -X GET "$BASE_URL/api/users/me" \
      -H "Authorization: Bearer $JWT_TOKEN" | format_json

    echo -e "\n${YELLOW}7. Get User by ID${NC}"
    curl -s -X GET "$BASE_URL/api/users/1" \
      -H "Authorization: Bearer $JWT_TOKEN" | format_json

    echo -e "\n${YELLOW}8. List All Users (Requires ADMIN Role - will fail with regular user)${NC}"
    curl -s -X GET "$BASE_URL/api/users?page=0&size=20" \
      -H "Authorization: Bearer $JWT_TOKEN" | format_json
else
    echo -e "${RED}Skipping user endpoints - no JWT token available${NC}"
fi

echo -e "\n=== Project Endpoints (Requires JWT) ==="

if [ -n "$JWT_TOKEN" ]; then
    PROJECT_ID=""

    echo -e "${YELLOW}9. Create Project${NC}"
    CREATE_PROJECT=$(curl -s -X POST "$BASE_URL/api/projects" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $JWT_TOKEN" \
      -d '{
        "name": "Test Project",
        "description": "A test project created via API"
      }')
    echo "$CREATE_PROJECT" | format_json

    # Extract project ID
    if [ "$JQ_AVAILABLE" = true ]; then
        PROJECT_ID=$(echo "$CREATE_PROJECT" | jq -r '.data.id // empty')
    else
        PROJECT_ID=$(echo "$CREATE_PROJECT" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi

    if [ -n "$PROJECT_ID" ] && [ "$PROJECT_ID" != "null" ]; then
        echo -e "${GREEN}Project created with ID: $PROJECT_ID${NC}"

        echo -e "\n${YELLOW}10. Get Project by ID${NC}"
        curl -s -X GET "$BASE_URL/api/projects/$PROJECT_ID" \
          -H "Authorization: Bearer $JWT_TOKEN" | format_json

        echo -e "\n${YELLOW}11. List All Projects (Paginated)${NC}"
        curl -s -X GET "$BASE_URL/api/projects?page=0&size=10&sort=createdAt,desc" \
          -H "Authorization: Bearer $JWT_TOKEN" | format_json

        echo -e "\n${YELLOW}12. Update Project${NC}"
        curl -s -X PUT "$BASE_URL/api/projects/$PROJECT_ID" \
          -H "Content-Type: application/json" \
          -H "Authorization: Bearer $JWT_TOKEN" \
          -d '{
            "name": "Updated Project Name",
            "description": "Updated description via API"
          }' | format_json

        echo -e "\n${YELLOW}13. Delete Project (Requires ADMIN Role - will fail with regular user)${NC}"
        curl -s -X DELETE "$BASE_URL/api/projects/$PROJECT_ID" \
          -H "Authorization: Bearer $JWT_TOKEN" | format_json
    else
        echo -e "${RED}Failed to extract project ID${NC}"
    fi
else
    echo -e "${RED}Skipping project endpoints - no JWT token available${NC}"
fi

echo -e "\n=== Validation Error Testing ==="

echo -e "${YELLOW}14. Test Validation Error (Missing required fields)${NC}"
curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{}' | format_json

echo -e "\n${YELLOW}15. Test Validation Error (Invalid email)${NC}"
curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "invalid-email",
    "password": "password123"
  }' | format_json

echo -e "\n${YELLOW}16. Test Validation Error (Short password)${NC}"
curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test2@example.com",
    "password": "short"
  }' | format_json

echo -e "\n${YELLOW}17. Test Project Validation Error (Missing name)${NC}"
if [ -n "$JWT_TOKEN" ]; then
    curl -s -X POST "$BASE_URL/api/projects" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $JWT_TOKEN" \
      -d '{
        "description": "Project without name"
      }' | format_json
else
    echo -e "${RED}Skipping - no JWT token${NC}"
fi

echo -e "\n${YELLOW}18. Test 401 Unauthorized (No JWT)${NC}"
curl -s -X GET "$BASE_URL/api/users/me" | format_json

echo -e "\n${YELLOW}19. Test 404 Not Found${NC}"
if [ -n "$JWT_TOKEN" ]; then
    curl -s -X GET "$BASE_URL/api/projects/999999" \
      -H "Authorization: Bearer $JWT_TOKEN" | format_json
else
    curl -s -X GET "$BASE_URL/api/projects/999999" | format_json
fi

echo -e "\n=== Test Summary ==="
echo -e "${GREEN}All API tests completed!${NC}"
echo ""
echo "Notes:"
echo "- Admin endpoints require a user with ADMIN role"
echo "- JWT tokens expire after 24 hours"
echo "- Run with a PostgreSQL database running on localhost:5432"
echo "- Ensure application is running on port 8080"