#!/bin/bash

# ZIBOTO BACKEND STARTUP SCRIPT
# This script ensures all environment variables are properly loaded

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo -e "${BLUE}╔══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                                          ║${NC}"
echo -e "${BLUE}║       ${GREEN}ZIBOTO BACKEND STARTUP${BLUE}            ║${NC}"
echo -e "${BLUE}║                                          ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════╝${NC}"
echo ""

# Step 1: Check if .env exists
if [ ! -f .env ]; then
    echo -e "${RED}✗ Error: .env file not found!${NC}"
    echo -e "${YELLOW}Creating .env file with default values...${NC}"
    
    JWT_SECRET_VAL=$(openssl rand -base64 64 | tr -d '\n')
    
    cat > .env << EOF
# Ziboto Backend Environment Variables
JWT_SECRET=$JWT_SECRET_VAL
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
DATABASE_URL=jdbc:postgresql://localhost:5433/ziboto
DATABASE_USERNAME=ziboto
DATABASE_PASSWORD=ziboto123
REDIS_HOST=localhost
REDIS_PORT=6380
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
EOF
    echo -e "${GREEN}✓ .env file created${NC}"
fi

# Step 2: Load environment variables
echo -e "${YELLOW}[1/5] Loading environment variables...${NC}"
set -a  # Mark all created variables for export
source .env
set +a
echo -e "${GREEN}✓ Environment variables loaded${NC}"

# Step 3: Verify critical variables
echo -e "${YELLOW}[2/5] Verifying configuration...${NC}"

if [ -z "$JWT_SECRET" ]; then
    echo -e "${RED}✗ Error: JWT_SECRET is not set!${NC}"
    exit 1
fi

echo -e "${GREEN}✓ JWT_SECRET: ${JWT_SECRET:0:25}... (truncated)${NC}"
echo -e "${GREEN}✓ DATABASE_URL: $DATABASE_URL${NC}"
echo -e "${GREEN}✓ REDIS_HOST: $REDIS_HOST:$REDIS_PORT${NC}"
echo -e "${GREEN}✓ SERVER_PORT: $SERVER_PORT${NC}"

# Step 4: Check if PostgreSQL is running
echo -e "${YELLOW}[3/5] Checking PostgreSQL...${NC}"
if docker ps | grep -q ziboto-postgres; then
    echo -e "${GREEN}✓ PostgreSQL is running${NC}"
else
    echo -e "${YELLOW}⚠  PostgreSQL not running. Starting...${NC}"
    docker-compose up -d postgres
    echo -e "${GREEN}✓ PostgreSQL started${NC}"
    sleep 3
fi

# Step 5: Check if Redis is running
echo -e "${YELLOW}[4/5] Checking Redis...${NC}"
if docker ps | grep -q ziboto-redis; then
    echo -e "${GREEN}✓ Redis is running${NC}"
else
    echo -e "${YELLOW}⚠  Redis not running. Starting...${NC}"
    docker-compose up -d redis
    echo -e "${GREEN}✓ Redis started${NC}"
    sleep 2
fi

# Step 6: Start application
echo -e "${YELLOW}[5/5] Starting Spring Boot application...${NC}"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${GREEN}Application will be available at:${NC}"
echo -e "  ${BLUE}→${NC} http://localhost:$SERVER_PORT"
echo -e "  ${BLUE}→${NC} http://localhost:$SERVER_PORT/swagger-ui.html"
echo -e "  ${BLUE}→${NC} http://localhost:$SERVER_PORT/actuator/health"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo ""

# Run with environment variables
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--app.security.jwt.secret=$JWT_SECRET"
