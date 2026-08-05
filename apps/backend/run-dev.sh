#!/bin/bash

##
# Development startup script for Ziboto Backend
# Loads environment variables from .env and starts the Spring Boot application
##

# Load .env file
if [ -f .env ]; then
    echo "Loading environment variables from .env..."
    set -a
    source .env
    set +a
    echo "✓ Environment variables loaded"
else
    echo "⚠️  Warning: .env file not found. Copy .env.example to .env and configure it."
    exit 1
fi

# Check if JWT_SECRET is set
if [ -z "$JWT_SECRET" ]; then
    echo "❌ Error: JWT_SECRET is not configured!"
    echo "Run: bash scripts/generate-jwt-secret.sh"
    exit 1
fi

echo "Starting Ziboto Backend..."
echo ""

# Run Spring Boot application
mvn spring-boot:run
