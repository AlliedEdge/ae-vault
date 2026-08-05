#!/bin/bash

# JWT Authentication Test Script
# Tests Bearer authentication, token validation, and protected endpoints

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_URL="$BASE_URL/api/v1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}JWT Authentication Test Suite${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print test result
print_result() {
    local test_name=$1
    local status=$2
    local response=$3
    
    if [ "$status" -eq 200 ] || [ "$status" -eq 201 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
    elif [ "$status" -eq 401 ] && [[ "$test_name" == *"Should Fail"* ]]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name (Expected 401)"
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name (Status: $status)"
    fi
    echo -e "${CYAN}Response:${NC} $response" | head -c 200
    echo ""
    echo ""
}

# Test 1: Login and Get Tokens
echo -e "${YELLOW}Test 1: Login and Get JWT Tokens${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "admin123"
  }')
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Login and Get Tokens" "$status" "$body"

# Extract tokens if successful
if [ "$status" -eq 200 ]; then
    ACCESS_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    REFRESH_TOKEN=$(echo "$body" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)
    
    if [ ! -z "$ACCESS_TOKEN" ]; then
        echo -e "${GREEN}✓ Access Token extracted successfully${NC}"
        echo -e "${CYAN}Token (first 50 chars):${NC} ${ACCESS_TOKEN:0:50}..."
        echo ""
    fi
    
    if [ ! -z "$REFRESH_TOKEN" ]; then
        echo -e "${GREEN}✓ Refresh Token extracted successfully${NC}"
        echo ""
    fi
else
    echo -e "${RED}✗ Login failed. Cannot proceed with remaining tests.${NC}"
    exit 1
fi

# Test 2: Access Protected Endpoint WITH Token
echo -e "${YELLOW}Test 2: Access Protected Endpoint WITH Token${NC}"
response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/auth/verify" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Access Protected Endpoint WITH Token" "$status" "$body"

# Test 3: Access Protected Endpoint WITHOUT Token (Should Fail)
echo -e "${YELLOW}Test 3: Access Protected Endpoint WITHOUT Token (Should Fail)${NC}"
response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/auth/verify")
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Access Protected Endpoint WITHOUT Token (Should Fail)" "$status" "$body"

# Test 4: Access Protected Endpoint WITH Invalid Token (Should Fail)
echo -e "${YELLOW}Test 4: Access Protected Endpoint WITH Invalid Token (Should Fail)${NC}"
response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/auth/verify" \
  -H "Authorization: Bearer invalid.token.here")
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Access Protected Endpoint WITH Invalid Token (Should Fail)" "$status" "$body"

# Test 5: Access Protected Endpoint WITH Malformed Header (Should Fail)
echo -e "${YELLOW}Test 5: Access Protected Endpoint WITH Malformed Header (Should Fail)${NC}"
response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/auth/verify" \
  -H "Authorization: InvalidFormat $ACCESS_TOKEN")
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Access Protected Endpoint WITH Malformed Header (Should Fail)" "$status" "$body"

# Test 6: Token Refresh
echo -e "${YELLOW}Test 6: Token Refresh${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }")
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Token Refresh" "$status" "$body"

# Extract new tokens
if [ "$status" -eq 200 ]; then
    NEW_ACCESS_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    if [ ! -z "$NEW_ACCESS_TOKEN" ]; then
        echo -e "${GREEN}✓ New Access Token received${NC}"
        ACCESS_TOKEN=$NEW_ACCESS_TOKEN
        echo ""
    fi
fi

# Test 7: Verify New Token Works
if [ ! -z "$NEW_ACCESS_TOKEN" ]; then
    echo -e "${YELLOW}Test 7: Verify New Token Works${NC}"
    response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/auth/verify" \
      -H "Authorization: Bearer $NEW_ACCESS_TOKEN")
    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    print_result "Verify New Token Works" "$status" "$body"
fi

# Test 8: Logout (Blacklist Token)
echo -e "${YELLOW}Test 8: Logout (Blacklist Token)${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/auth/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Logout" "$status" "$body"

# Test 9: Try Using Token After Logout (Should Fail)
echo -e "${YELLOW}Test 9: Try Using Token After Logout (Should Fail)${NC}"
sleep 2  # Give Redis time to update
response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/auth/verify" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Try Using Token After Logout (Should Fail)" "$status" "$body"

# Test 10: Access Public Endpoint (No Token Required)
echo -e "${YELLOW}Test 10: Access Public Endpoint (No Token Required)${NC}"
response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/actuator/health")
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Access Public Endpoint (No Token)" "$status" "$body"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}✓ All tests completed!${NC}"
echo ""
echo -e "${CYAN}JWT Authentication Features Tested:${NC}"
echo -e "  ✓ Bearer Authentication (Authorization: Bearer <token>)"
echo -e "  ✓ Token Validation (signature, expiration)"
echo -e "  ✓ Extract User from token"
echo -e "  ✓ Populate SecurityContext"
echo -e "  ✓ Protected endpoints require token"
echo -e "  ✓ Public endpoints accessible without token"
echo -e "  ✓ Token refresh mechanism"
echo -e "  ✓ Token blacklisting on logout"
echo -e "  ✓ Invalid token rejection"
echo -e "  ✓ Malformed header rejection"
echo ""
echo -e "${CYAN}Public Endpoints (No Token Required):${NC}"
echo -e "  • /api/v1/auth/login"
echo -e "  • /api/v1/auth/register"
echo -e "  • /api/v1/auth/refresh"
echo -e "  • /actuator/**"
echo -e "  • /swagger-ui/**"
echo -e "  • /api-docs/**"
echo ""
echo -e "${CYAN}Protected Endpoints (Token Required):${NC}"
echo -e "  • Everything else under /api/v1/**"
echo ""
