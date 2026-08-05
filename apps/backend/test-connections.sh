#!/bin/bash

# Connection Test Script for Ziboto Backend
# Tests PostgreSQL and Redis connections

set -e

echo "=========================================="
echo "🔍 Ziboto Connection Test"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test 1: Check Docker containers
echo "${BLUE}1. Checking Docker containers...${NC}"
echo ""

if ! docker ps > /dev/null 2>&1; then
    echo "${RED}❌ Docker is not running${NC}"
    exit 1
fi

POSTGRES_RUNNING=$(docker ps --filter "ancestor=postgres:17" --format "{{.ID}}" | wc -l)
REDIS_RUNNING=$(docker ps --filter "ancestor=redis:7.4" --format "{{.ID}}" | wc -l)

if [ "$POSTGRES_RUNNING" -eq 0 ]; then
    echo "${RED}❌ PostgreSQL container is not running${NC}"
    echo "   Run: cd infra/docker && docker-compose up -d"
    exit 1
else
    echo "${GREEN}✅ PostgreSQL container is running${NC}"
fi

if [ "$REDIS_RUNNING" -eq 0 ]; then
    echo "${RED}❌ Redis container is not running${NC}"
    echo "   Run: cd infra/docker && docker-compose up -d"
    exit 1
else
    echo "${GREEN}✅ Redis container is running${NC}"
fi

echo ""

# Test 2: PostgreSQL connection
echo "${BLUE}2. Testing PostgreSQL connection (port 5433)...${NC}"
echo ""

if command -v psql &> /dev/null; then
    if PGPASSWORD=ziboto123 psql -h localhost -p 5433 -U ziboto -d ziboto -c "SELECT version();" > /dev/null 2>&1; then
        echo "${GREEN}✅ PostgreSQL connection successful${NC}"
        PGPASSWORD=ziboto123 psql -h localhost -p 5433 -U ziboto -d ziboto -c "SELECT version();" | head -3
    else
        echo "${RED}❌ PostgreSQL connection failed${NC}"
        echo "   Check: application.yml has url with port 5433"
    fi
else
    echo "${YELLOW}⚠️  psql not installed, testing via Docker...${NC}"
    POSTGRES_CONTAINER=$(docker ps --filter "ancestor=postgres:17" --format "{{.ID}}" | head -1)
    if docker exec "$POSTGRES_CONTAINER" psql -U ziboto -d ziboto -c "SELECT 1;" > /dev/null 2>&1; then
        echo "${GREEN}✅ PostgreSQL connection successful (via Docker)${NC}"
    else
        echo "${RED}❌ PostgreSQL connection failed${NC}"
    fi
fi

echo ""

# Test 3: Redis connection
echo "${BLUE}3. Testing Redis connection (port 6380)...${NC}"
echo ""

if command -v redis-cli &> /dev/null; then
    if redis-cli -h localhost -p 6380 PING > /dev/null 2>&1; then
        echo "${GREEN}✅ Redis connection successful${NC}"
        echo "   Response: $(redis-cli -h localhost -p 6380 PING)"
    else
        echo "${RED}❌ Redis connection failed${NC}"
        echo "   Check: application.yml has port 6380"
    fi
else
    echo "${YELLOW}⚠️  redis-cli not installed, testing via Docker...${NC}"
    REDIS_CONTAINER=$(docker ps --filter "ancestor=redis:7.4" --format "{{.ID}}" | head -1)
    if docker exec "$REDIS_CONTAINER" redis-cli PING > /dev/null 2>&1; then
        echo "${GREEN}✅ Redis connection successful (via Docker)${NC}"
        echo "   Response: $(docker exec "$REDIS_CONTAINER" redis-cli PING)"
    else
        echo "${RED}❌ Redis connection failed${NC}"
    fi
fi

echo ""

# Test 4: Check application.yml configuration
echo "${BLUE}4. Checking application.yml configuration...${NC}"
echo ""

YAML_FILE="src/main/resources/application.yml"

if [ -f "$YAML_FILE" ]; then
    # Check PostgreSQL port
    if grep -q "localhost:5433" "$YAML_FILE"; then
        echo "${GREEN}✅ PostgreSQL port 5433 configured correctly${NC}"
    else
        echo "${RED}❌ PostgreSQL port not set to 5433${NC}"
        echo "   Update: url: jdbc:postgresql://localhost:5433/ziboto"
    fi
    
    # Check Redis port (this is trickier since it's not in the URL)
    if grep -A 3 "redis:" "$YAML_FILE" | grep -q "port.*6380"; then
        echo "${GREEN}✅ Redis port 6380 configured correctly${NC}"
    else
        echo "${RED}❌ Redis port not set to 6380${NC}"
        echo "   Update: port: \${REDIS_PORT:6380}"
    fi
else
    echo "${RED}❌ application.yml not found${NC}"
fi

echo ""

# Test 5: Spring Boot application test (if running)
echo "${BLUE}5. Testing Spring Boot connection endpoints...${NC}"
echo ""

if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "${YELLOW}Spring Boot application is running${NC}"
    echo ""
    
    # Test connection endpoint
    echo "Testing connection endpoint..."
    RESPONSE=$(curl -s http://localhost:8080/api/v1/test/connections)
    
    if echo "$RESPONSE" | grep -q "HEALTHY"; then
        echo "${GREEN}✅ All connections healthy${NC}"
        echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    else
        echo "${RED}❌ Some connections failed${NC}"
        echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    fi
else
    echo "${YELLOW}⚠️  Spring Boot application is not running${NC}"
    echo "   Start it with: ./mvnw spring-boot:run"
    echo "   Then test endpoints:"
    echo "   - http://localhost:8080/api/v1/test/connections"
    echo "   - http://localhost:8080/api/v1/test/postgres"
    echo "   - http://localhost:8080/api/v1/test/redis"
fi

echo ""
echo "=========================================="
echo "✨ Connection test completed"
echo "=========================================="
echo ""

# Summary
echo "${BLUE}Summary:${NC}"
echo "- PostgreSQL: Host port 5433 → Container port 5432"
echo "- Redis: Host port 6380 → Container port 6379"
echo "- pgAdmin: http://localhost:5050"
echo "- RedisInsight: http://localhost:5540"
echo ""

echo "${BLUE}Next steps:${NC}"
echo "1. Start Spring Boot: ./mvnw spring-boot:run"
echo "2. Test connections: curl http://localhost:8080/api/v1/test/connections"
echo "3. View Swagger UI: http://localhost:8080/swagger-ui.html"
