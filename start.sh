#!/bin/bash
# CacheIQ Startup Script
# Autoload environment variables from .env

set -a  # Auto-export all variables

# Load .env file if exists
if [ -f .env ]; then
    source .env
    echo "✓ Loaded environment from .env"
else
    echo "⚠ .env file not found. Creating from example..."
    cp .env.example .env
    echo "⚠ Please edit .env and add your GROQ_API_KEY"
    exit 1
fi

# Check required variables
if [ -z "$GROQ_API_KEY" ]; then
    echo "⚠ GROQ_API_KEY not set in .env"
    echo "⚠ Please edit .env and add your Groq API key"
    exit 1
fi

# Start the application
echo "✓ Starting CacheIQ..."
./gradlew bootRun