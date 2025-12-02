#!/usr/bin/env zsh

# 🚀 Run blockchain in PRODUCTION mode  
# Optimized logging for maximum performance

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

echo "🏭 Starting Private Blockchain in PRODUCTION mode..."
echo "⚡ Logging: Optimized for performance (WARN+ only)"

# Change to project root directory
cd "$SCRIPT_DIR/.."

# Clean database before running
cleanup_database

# Run with production profile (uses log4j2-production.xml automatically)
mvn clean compile -Pproduction
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pproduction

echo "✅ Production session completed"