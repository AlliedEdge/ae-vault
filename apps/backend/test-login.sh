#!/bin/bash

# Login Flow Test Script
# This script tests the complete login flow implementation

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_URL="$BASE_URL/api/v1/auth"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Login Flow Test Script${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Function to print test result
print_result() {
    local test_name=$1
    local status=$2
    local response=$3
    
    if [ "$status" -eq 200 ] || [ "$status" -eq 201 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name (Status: $status)"
    fi
    echo -e "Response: $response"
    echo ""
}

# Test 1: Valid Login
echo -e "${YELLOW}Test 1: Valid Login${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "admin123"
  }')
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
print_result "Valid Login" "$status" "$body"

# Extract tokens if successful
if [ "$status" -eq 200 ]; then
    ACCESS_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    REFRESH_TOKEN=$(echo "$body" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)
    
    if [ ! -z "$ACCESS_TOKEN" ]; then
        echo -e "${GREEN}Access Token extracted successfully${NC}"
        echo -e "${BLUE}Token preview: ${ACCESS_TOKEN:0:50}...${NC}"
        echo ""
    fi
fi

# Test 2: Invalid Password
echo -e "${YELLOW}Test 2: Invalid Password (Should Fail)${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "wrongpassword"
  }')
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$status" -eq 401 ]; then
    echo -e "${GREEN}✓ PASS${NC}: Invalid password correctly rejected (Status: 401)"
else
    echo -e "${RED}✗ FAIL${NC}: Expected 401, got $status"
fi
echo -e "Response: $body"
echo ""

# Test 3: Invalid Username
echo -e "${YELLOW}Test 3: Invalid Username (Should Fail)${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "nonexistentuser",
    "password": "password123"
  }')
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$status" -eq 401 ]; then
    echo -e "${GREEN}✓ PASS${NC}: Invalid username correctly rejected (Status: 401)"
else
    echo -e "${RED}✗ FAIL${NC}: Expected 401, got $status"
fi
echo -e "Response: $body"
echo ""

# Test 4: Rate Limiting (6 rapid failed attempts)
echo -e "${YELLOW}Test 4: Rate Limiting (6 rapid failed attempts)${NC}"
echo "Making 6 rapid failed login attempts..."
for i in {1..6}; do
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/login" \
      -H "Content-Type: application/json" \
      -d '{
        "usernameOrEmail": "ratelimitest",
        "password": "wrongpassword"
      }')
    status=$(echo "$response" | tail -n1)
    echo "Attempt $i: Status $status"
done
echo ""
echo "7th attempt should be rate limited..."
response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "ratelimitest",
    "password": "wrongpassword"
  }')
status=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)
if [ "$status" -eq 429 ]; then
    echo -e "${GREEN}✓ PASS${NC}: Rate limiting working (Status: 429)"
else
    echo -e "${YELLOW}⚠ INFO${NC}: Rate limit status: $status (expected 429)"
fi
echo -e "Response: $body"
echo ""

# Test 5: Token Verification (if we have an access token)
if [ ! -z "$ACCESS_TOKEN" ]; then
    echo -e "${YELLOW}Test 5: Token Verification${NC}"
    response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/verify" \
      -H "Authorization: Bearer $ACCESS_TOKEN")
    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    print_result "Token Verification" "$status" "$body"
fi

# Test 6: Token Refresh (if we have a refresh token)
if [ ! -z "$REFRESH_TOKEN" ]; then
    echo -e "${YELLOW}Test 6: Token Refresh${NC}"
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/refresh" \
      -H "Content-Type: application/json" \
      -d "{
        \"refreshToken\": \"$REFRESH_TOKEN\"
      }")
    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    print_result "Token Refresh" "$status" "$body"
fi

# Test 7: Logout (if we have an access token)
if [ ! -z "$ACCESS_TOKEN" ]; then
    echo -e "${YELLOW}Test 7: Logout${NC}"
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/logout" \
      -H "Authorization: Bearer $ACCESS_TOKEN")
    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    print_result "Logout" "$status" "$body"
    
    # Test 8: Verify token is blacklisted after logout
    echo -e "${YELLOW}Test 8: Token After Logout (Should Fail)${NC}"
    response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/verify" \
      -H "Authorization: Bearer $ACCESS_TOKEN")
    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    if [ "$status" -eq 401 ]; then
        echo -e "${GREEN}✓ PASS${NC}: Token correctly invalidated after logout (Status: 401)"
    else
        echo -e "${RED}✗ FAIL${NC}: Token should be invalid after logout, got status $status"
    fi
    echo -e "Response: $body"
    echo ""
fi

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}================================${NC}"
echo ""
echo -e "All tests completed!"
echo ""
echo -e "${YELLOW}To verify in Redis:${NC}"
echo -e "  redis-cli GET session:user:admin"
echo -e "  redis-cli GET rate_limit:login:ratelimitest"
echo ""
echo -e "${YELLOW}To verify in Database:${NC}"
echo -e "  SELECT username, last_login_at FROM users WHERE username = 'admin';"
echo -e "  SELECT * FROM audit_logs WHERE action = 'LOGIN' ORDER BY created_at DESC LIMIT 5;"
echo -e "  SELECT * FROM refresh_tokens ORDER BY created_at DESC LIMIT 5;"
echo ""
