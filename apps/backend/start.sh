#!/bin/bash
# Simple startup script - just run this!

# Load JWT_SECRET from .env
if [ -f .env ]; then
    export $(grep JWT_SECRET .env | grep -v '^#')
fi

# Start application with JWT secret passed as argument
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.security.jwt.secret=${JWT_SECRET}"
