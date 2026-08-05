#!/bin/bash

##
# Generate a secure JWT secret key for Ziboto
# The key is Base64 encoded and 256 bits (32 bytes) for HS512 algorithm
##

echo "Generating secure JWT secret key..."
echo ""

# Generate 32 random bytes and encode to Base64
JWT_SECRET=$(openssl rand -base64 32)

echo "Your JWT secret key:"
echo "===================="
echo "$JWT_SECRET"
echo ""
echo "Add this to your .env file:"
echo "JWT_SECRET=$JWT_SECRET"
echo ""
echo "Or export as environment variable:"
echo "export JWT_SECRET=\"$JWT_SECRET\""
echo ""
echo "⚠️  IMPORTANT: Keep this secret secure and never commit it to version control!"
