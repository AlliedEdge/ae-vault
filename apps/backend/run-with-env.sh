#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Starting Ziboto Backend...${NC}"

# Load environment variables from .env file
if [ -f .env ]; then
    echo -e "${GREEN}✓ Loading environment variables from .env${NC}"
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
else
    echo -e "${YELLOW}⚠ .env file not found!${NC}"
fi

# Verify JWT_SECRET is set
if [ -z "$JWT_SECRET" ]; then
    echo -e "${YELLOW}⚠ JWT_SECRET not set in environment!${NC}"
    echo "Generating a temporary JWT secret..."
    export JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    echo -e "${GREEN}✓ Temporary JWT_SECRET generated${NC}"
fi

echo -e "${GREEN}✓ Environment configured${NC}"
echo ""
echo "JWT_SECRET: ${JWT_SECRET:0:20}... (truncated)"
echo "DATABASE_URL: $DATABASE_URL"
echo "REDIS_HOST: $REDIS_HOST"
echo "SERVER_PORT: $SERVER_PORT"
echo ""
echo -e "${YELLOW}Starting Spring Boot application...${NC}"
echo ""

# Run Spring Boot application
./mvnw spring-boot:run
